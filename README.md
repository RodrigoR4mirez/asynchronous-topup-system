# 📱 Asynchronous Top-Up System

Sistema de recarga telefónica basado en eventos construido con **Quarkus 3** y **Java 21**. Tres microservicios independientes se comunican vía Kafka (serialización Avro) y comparten una base de datos MySQL.

![Arquitectura del Sistema](./files/Diagrama.jpeg)

## 🎯 Resumen 

**Arquitectura event-driven** con 3 microservicios desacoplados:

**📥 Componentes:**
- **sync-topup-api-v1** (REST) → Recibe solicitudes y persiste en MySQL como `PENDING`
- **async-topup-producer-v1** (Scheduler) → Polling cada 10s, publica a Kafka, marca `SENT_TO_KAFKA`
- **async-topup-consumer-v1** (Consumer) → Procesa eventos, valida saldo, actualiza `COMPLETED`/`FAILED`

**💡 Decisiones Clave:**
- **¿Por qué scheduler vs Kafka directo?** 
  - Cliente recarga $50 → API confirma `202` al instante aunque Kafka esté caído
  - Scheduler reintenta automáticamente sin duplicar ni perder datos
  - Analogía: Como comprar boleto de avión (venta inmediata, emisión después)

- **¿Por qué Kafka?**
  - Desacoplamiento para escalar componentes independientemente
  - Garantía de entrega y absorción de picos de tráfico
  - Schema Registry (Avro) asegura compatibilidad entre versiones

**🔄 Flujo:** Cliente → API → MySQL (`PENDING`) → Scheduler → Kafka → Consumer → MySQL (`COMPLETED`/`FAILED`)

---

## 🏗️ Arquitectura

```
┌─────────────────────┐     ┌─────────────────────────┐     ┌─────────────────────┐
│ sync-topup-api-v1   │     │ async-topup-producer-v1  │     │ async-topup         │
│ (REST API)          │     │ (Scheduler/Producer)     │     │ -consumer-v1        │
│ :8084               │     │ :8085                    │     │ (Kafka Consumer)    │
│                     │     │                          │     │ :8086               │
└─────────┬────────────┘     └────────────┬─────────────┘     └──────────┬──────────┘
          │                            │                            │
          │  persist PENDING           │ poll PENDING               │ consume TopUpEvent
          │                            │ emit to Kafka              │ validate balance
          │                            │ update SENT_TO_KAFKA       │ update COMPLETED/FAILED
          ▼                            ▼                            ▼
     ┌─────────────────────────────────────────────────────────────────┐
     │                     MySQL (phone_recharge_db)                   │
     │   Tables: recharge_requests, balance_wallets, process_audits    │
     └─────────────────────────────────────────────────────────────────┘
                                │
                     ┌──────────┴──────────┐
                     │   Kafka + Schema    │
                     │   Registry (Avro)   │
                     │   Topic: topup-topic│
                     └─────────────────────┘
```

### Flujo de Estados
`PENDING` → `SENT_TO_KAFKA` → `COMPLETED` | `FAILED`

---

## 🛠️ Componentes

### 1. sync-topup-api-v1 (REST API)
- **Puerto:** 8084
- **Responsabilidad:** Punto de entrada REST. Valida y persiste solicitudes en estado `PENDING`
- **Endpoint:** `POST http://localhost:8084/v1/topups`
- **Ejemplo:**
  ```json
  {
    "phoneNumber": "987654321",
    "amount": 50.0,
    "carrier": "MOVISTAR"
  }
  ```
- **Respuesta:** `202 Accepted`

### 2. async-topup-producer-v1 (Scheduler/Producer)
- **Puerto:** 8085
- **Responsabilidad:** Escanea cada 10 segundos solicitudes `PENDING`, las publica a Kafka y actualiza estado a `SENT_TO_KAFKA`
- **Mecanismo:** `@Scheduled(every = "10s")`
- **Output:** Eventos Avro al tópico `topup-topic`

### 3. async-topup-consumer-v1 (Consumer)
- **Puerto:** 8086
- **Responsabilidad:** Consume eventos de Kafka, valida saldo disponible y actualiza estado final (`COMPLETED` o `FAILED`)
- **Input:** Mensajes Avro desde `topup-topic`
- **Acción:** Descuenta saldo y registra auditoría

---

## 📋 Prerrequisitos

- **Docker** instalado
- **MySQL/MariaDB** corriendo:
  - Contenedor: `mariadb10432`
  - Puerto: `3307`
  - Base de datos: `phone_recharge_db`
  - Usuario/Password: `root`/`123456789`
- **Kafka + Schema Registry** corriendo:
  - Brokers: `localhost:19092,29092`
  - Schema Registry: `localhost:8081`

---

## 🚀 Ejecución con Docker - Paso a Paso

### Paso 1: Verificar Infraestructura

#### 1.1 Verificar MySQL
```bash
# Verificar si el contenedor está corriendo
docker ps | grep mariadb10432

# Si no está corriendo, iniciarlo
docker start mariadb10432

# Verificar tablas
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "SHOW TABLES;"
```

#### 1.2 Verificar Kafka
```bash
# Verificar brokers
docker ps | grep kafka

# Verificar Schema Registry
curl http://localhost:8081/subjects

# Crear el nuevo tópico para recargas (2 particiones)
docker exec -it kafka-broker-1 kafka-topics   --bootstrap-server kafka-broker-1:9092,kafka-broker-2:9092   --if-not-exists   --create   --topic topup-topic   --partitions 2   --replication-factor 2

### Eliminar tópico
docker exec -it kafka-broker-1 kafka-topics   --bootstrap-server localhost:19092   --delete   --topic topup-topic
```


### Paso 2: Construir Imágenes Docker

```bash
# Construir sync-topup-api-v1
docker build -t sync-topup-api-v1:latest \
  -f sync-topup-api-v1/Dockerfile \
  sync-topup-api-v1/

# Construir async-topup-producer-v1
docker build -t async-topup-producer-v1:latest \
  -f async-topup-producer-v1/Dockerfile \
  async-topup-producer-v1/

# Construir async-topup-consumer-v1
docker build -t async-topup-consumer-v1:latest \
  -f async-topup-consumer-v1/Dockerfile \
  async-topup-consumer-v1/

# Verificar imágenes creadas
docker images | grep topup
```

### Paso 3: Ejecutar Contenedores

#### 3.1 Ejecutar sync-topup-api-v1 (REST API)
```bash
docker run -d \
  --name sync-api \
  -p 8084:8084 \
  -e DB_HOST=192.168.18.29 \
  -e DB_PORT=3307 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=123456789 \
  sync-topup-api-v1:latest
```

#### 3.2 Ejecutar async-topup-producer-v1 (Producer/Scheduler)
```bash
docker run -d \
  --name async-producer \
  -p 8085:8085 \
  -e DB_HOST=192.168.18.29 \
  -e DB_PORT=3307 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=123456789 \
  -e KAFKA_BROKERS=PLAINTEXT://192.168.18.29:19092,PLAINTEXT://192.168.18.29:29092 \
  -e SCHEMA_REGISTRY_URL=http://192.168.18.29:8081 \
  async-topup-producer-v1:latest
```

#### 3.3 Ejecutar async-topup-consumer-v1 (Consumer)
```bash
docker run -d \
  --name async-consumer \
  -p 8086:8086 \
  -e CONFIG_DB_HOST=192.168.18.29:3307 \
  -e CONFIG_DB_USERNAME=root \
  -e CONFIG_DB_PASSWORD=123456789 \
  -e KAFKA_BROKERS=PLAINTEXT://192.168.18.29:19092,PLAINTEXT://192.168.18.29:29092 \
  -e SCHEMA_REGISTRY_URL=http://192.168.18.29:8081 \
  async-topup-consumer-v1:latest
```

### Paso 4: Verificar Contenedores

```bash
# Ver contenedores corriendo
docker ps | grep topup

# Ver logs en tiempo real
docker logs -f sync-api
docker logs -f async-producer
docker logs -f async-consumer
```

### Paso 5: Probar el Sistema

```bash
# Crear solicitud de recarga
curl -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "987654321",
    "amount": 50.0,
    "carrier": "MOVISTAR"
  }'

# Debe retornar: 202 Accepted
```

**Verificar en la base de datos:**
```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db \
  -e "SELECT id, phone_number, amount, status, created_at FROM recharge_requests ORDER BY created_at DESC LIMIT 5;"
```

---

## 🛑 Detener y Eliminar Contenedores

### Detener Servicios
```bash
docker stop sync-api async-producer async-consumer
```

### Eliminar Contenedores
```bash
docker rm sync-api async-producer async-consumer
```

### Eliminar Imágenes (opcional)
```bash
docker rmi sync-topup-api-v1:latest async-topup-producer-v1:latest async-topup-consumer-v1:latest
```

### Limpiar Todo de una Vez
```bash
# Detener y eliminar contenedores
docker stop sync-api async-producer async-consumer
docker rm sync-api async-producer async-consumer

# Eliminar imágenes
docker rmi sync-topup-api-v1:latest async-topup-producer-v1:latest async-topup-consumer-v1:latest

# (Opcional) Limpiar sistema
docker system prune -f
```

---

## 🔄 Reiniciar Sistema Completo

Si eliminaste todo y quieres volver a levantar:

```bash
# 1. Construir imágenes
docker build -t sync-topup-api-v1:latest -f sync-topup-api-v1/Dockerfile sync-topup-api-v1/
docker build -t async-topup-producer-v1:latest -f async-topup-producer-v1/Dockerfile async-topup-producer-v1/
docker build -t async-topup-consumer-v1:latest -f async-topup-consumer-v1/Dockerfile async-topup-consumer-v1/

# 2. Ejecutar sync-api
docker run -d \
  --name sync-api \
  -p 8084:8084 \
  -e DB_HOST=192.168.18.29 \
  -e DB_PORT=3307 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=123456789 \
  sync-topup-api-v1:latest

# 3. Ejecutar async-producer
docker run -d \
  --name async-producer \
  -p 8085:8085 \
  -e DB_HOST=192.168.18.29 \
  -e DB_PORT=3307 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=123456789 \
  -e KAFKA_BROKERS=PLAINTEXT://192.168.18.29:19092,PLAINTEXT://192.168.18.29:29092 \
  -e SCHEMA_REGISTRY_URL=http://192.168.18.29:8081 \
  async-topup-producer-v1:latest

# 4. Ejecutar async-consumer
docker run -d \
  --name async-consumer \
  -p 8086:8086 \
  -e CONFIG_DB_HOST=192.168.18.29:3307 \
  -e CONFIG_DB_USERNAME=root \
  -e CONFIG_DB_PASSWORD=123456789 \
  -e KAFKA_BROKERS=PLAINTEXT://192.168.18.29:19092,PLAINTEXT://192.168.18.29:29092 \
  -e SCHEMA_REGISTRY_URL=http://192.168.18.29:8081 \
  async-topup-consumer-v1:latest

# 5. Verificar
docker ps | grep topup
```

---

## ⚙️ Variables de Entorno

### sync-topup-api-v1
| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_HOST` | localhost | Host de MySQL |
| `DB_PORT` | 3307 | Puerto de MySQL |
| `DB_USERNAME` | root | Usuario de base de datos |
| `DB_PASSWORD` | 123456789 | Contraseña de base de datos |

### async-topup-producer-v1
| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_HOST` | localhost | Host de MySQL |
| `DB_PORT` | 3307 | Puerto de MySQL |
| `DB_USERNAME` | root | Usuario de base de datos |
| `DB_PASSWORD` | 123456789 | Contraseña de base de datos |
| `KAFKA_BROKERS` | PLAINTEXT://localhost:19092,PLAINTEXT://localhost:29092 | Brokers de Kafka |
| `SCHEMA_REGISTRY_URL` | http://localhost:8081 | URL del Schema Registry |

### async-topup-consumer-v1
| Variable | Default | Descripción |
|----------|---------|-------------|
| `CONFIG_DB_HOST` | localhost:3307 | Host:Puerto de MySQL |
| `CONFIG_DB_NAME` | phone_recharge_db | Nombre de la base de datos |
| `CONFIG_DB_USERNAME` | root | Usuario de base de datos |
| `CONFIG_DB_PASSWORD` | 123456789 | Contraseña de base de datos |
| `KAFKA_BROKERS` | PLAINTEXT://localhost:19092,PLAINTEXT://localhost:29092 | Brokers de Kafka |
| `SCHEMA_REGISTRY_URL` | http://localhost:8081 | URL del Schema Registry |

---

## 🧪 Comandos Útiles

### Ver logs en tiempo real
```bash
docker logs -f sync-api
docker logs -f async-producer
docker logs -f async-consumer
```

### Inspeccionar contenedor
```bash
docker inspect sync-api
```

### Ejecutar comandos dentro del contenedor
```bash
docker exec -it sync-api /bin/bash
```

### Ver uso de recursos
```bash
docker stats sync-api async-producer async-consumer
```

---

## 📝 Notas

- Se usa `--network host` para facilitar la comunicación con servicios locales (MySQL, Kafka)
- Los puertos son: **8084** (API), **8085** (Producer), **8086** (Consumer)
- El Producer ejecuta un scheduler cada 10 segundos para procesar recargas pendientes
- El Consumer procesa eventos de Kafka en tiempo real

---

## 🗄️ Modelo de Datos

### Tabla: recharge_requests
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | BIGINT (PK) | Identificador único |
| `recharge_id` | VARCHAR(36) | UUID de la transacción |
| `phone_number` | VARCHAR(15) | Número de teléfono |
| `amount` | DECIMAL(10,2) | Monto de la recarga |
| `carrier` | VARCHAR(50) | Operador (MOVISTAR, CLARO, etc) |
| `status` | VARCHAR(20) | PENDING, SENT_TO_KAFKA, COMPLETED, FAILED |
| `created_at` | DATETIME | Fecha de creación |
| `updated_at` | DATETIME | Fecha de actualización |

### Tabla: balance_wallets
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | BIGINT (PK) | Identificador único |
| `operator_name` | VARCHAR(50) | Nombre del operador |
| `current_balance` | DECIMAL(15,2) | Saldo disponible |
| `currency` | VARCHAR(3) | Moneda (PEN, USD) |

### Tabla: process_audits
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | BIGINT (PK) | Identificador único |
| `recharge_id` | VARCHAR(36) | UUID de la transacción |
| `completion_date` | DATETIME | Fecha de finalización |
| `error_details` | TEXT | Detalles del proceso/error |

---

# PASOS de BD

## los INSERT organizados por el momento en que cada microservicio actuaría.

------------------------------------------------------------------------

## Paso 1: Configuración Inicial (Bolsa de Dinero)

Antes de empezar, necesitamos saldo en el sistema. Esto lo haría un
administrador.

``` sql
-- Ponemos dinero para los operadores en Perú
INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Movistar', 100.00, 'PEN');
INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Claro', 50.00, 'PEN');
```

------------------------------------------------------------------------

## Paso 2: Acción del Gateway API (Componente 1)

Cuando el usuario presiona "Recargar" en su app, tu primer servicio de
Quarkus ejecuta esto.\
Nota que el estado es PENDING.

``` sql
-- Simulamos una recarga de 20 soles
INSERT INTO recharge_requests (recharge_id, phone_number, amount, status) 
VALUES ('req-777-abc', '987654321', 20.00, 'PENDING');
```

------------------------------------------------------------------------

## Paso 3: Acción del Processor & Validator (Componente 3)

Después de que el mensaje viaja por Kafka, el tercer componente procesa
la lógica.

### Escenario A: Todo salió bien (Éxito)

El consumidor valida que hay saldo, descuenta los 20 soles de la bolsa y
actualiza la auditoría.

``` sql
-- 1. Descontamos el saldo (El Consumer lo hace)
UPDATE balance_wallets 
SET current_balance = current_balance - 20.00 
WHERE operator_name = 'Movistar';

-- 2. Marcamos como exitosa la solicitud
UPDATE recharge_requests 
SET status = 'SUCCESSFUL' 
WHERE recharge_id = 'req-777-abc';

-- 3. Llenamos la auditoría con el detalle del éxito
INSERT INTO process_audits (recharge_id, error_details) 
VALUES ('req-777-abc', 'Transaction processed by Kafka Consumer. Balance deducted from Movistar.');
```

------------------------------------------------------------------------

### Escenario B: No hay dinero suficiente (Fallo)

Imagina que alguien pide una recarga de 500 soles, pero solo tenemos
100.

``` sql
-- 1. Registramos la solicitud pendiente (C1)
INSERT INTO recharge_requests (recharge_id, phone_number, amount, status) 
VALUES ('req-999-xyz', '912345678', 500.00, 'PENDING');

-- 2. El Consumer (C3) detecta el error y actualiza a FAILED
UPDATE recharge_requests 
SET status = 'FAILED' 
WHERE recharge_id = 'req-999-xyz';

-- 3. Llenamos la auditoría explicando POR QUÉ falló
INSERT INTO process_audits (recharge_id, error_details) 
VALUES ('req-999-xyz', 'Insufficient balance. Required: 500.00, Available: 80.00 (Movistar)');
```

------------------------------------------------------------------------

## ¿Cómo verificar que todo se llenó bien?

Ejecuta este JOIN para ver la "película completa" de tus transacciones:

``` sql
SELECT 
    r.recharge_id, 
    r.phone_number, 
    r.amount, 
    r.status, 
    a.completion_date, 
    a.error_details
FROM recharge_requests r
LEFT JOIN process_audits a 
    ON r.recharge_id = a.recharge_id;
```

------------------------------------------------------------------------

---

# 🔧 Gestión de Base de Datos

## 🗄️ Consultas Rápidas a Base de Datos

Comandos útiles para gestionar las tablas directamente desde Docker.

### 📊 Consultar Todas las Tablas
```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"SELECT * FROM balance_wallets; \
SELECT * FROM process_audits; \
SELECT * FROM recharge_requests;"
```

### 🏗️ Crear Tablas
```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"CREATE TABLE IF NOT EXISTS recharge_requests ( \
  recharge_id VARCHAR(36) PRIMARY KEY, \
  phone_number VARCHAR(15) NOT NULL, \
  amount DECIMAL(10,2) NOT NULL, \
  carrier VARCHAR(20), \
  status VARCHAR(20) DEFAULT 'PENDING', \
  created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6), \
  updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) \
); \
\
CREATE TABLE IF NOT EXISTS balance_wallets ( \
  operator_id INT AUTO_INCREMENT PRIMARY KEY, \
  operator_name VARCHAR(50) NOT NULL, \
  current_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00, \
  currency VARCHAR(3) DEFAULT 'PEN' \
); \
\
CREATE TABLE IF NOT EXISTS process_audits ( \
  audit_id INT AUTO_INCREMENT PRIMARY KEY, \
  recharge_id VARCHAR(36), \
  completion_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, \
  error_details TEXT, \
  CONSTRAINT fk_recharge FOREIGN KEY (recharge_id) REFERENCES recharge_requests(recharge_id) \
);"
```

### 💰 Insertar Saldo Inicial
```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Movistar', 100.00, 'PEN'); \
INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Claro', 50.00, 'PEN');"
```

### 🧹 Limpiar Datos (Truncate)
```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"SET FOREIGN_KEY_CHECKS = 0; \
TRUNCATE TABLE process_audits; \
TRUNCATE TABLE recharge_requests; \
TRUNCATE TABLE balance_wallets; \
SET FOREIGN_KEY_CHECKS = 1;"
```

### 🗑️ Eliminar Datos (Delete)
```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"DELETE FROM process_audits; \
DELETE FROM recharge_requests;"
```

### ❌ Eliminar Tablas (Drop)
```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"DROP TABLE IF EXISTS process_audits; \
DROP TABLE IF EXISTS recharge_requests; \
DROP TABLE IF EXISTS balance_wallets;"
```

---

## 🌐 Pruebas con CURL

### Solicitud de Recarga con Formato Pretty
```bash
response=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"985725003","amount":10,"carrier":"CLARO"}')

code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')

[ -n "$body" ] && echo "$body" | jq . 2>/dev/null || echo "✓ $code Accepted"
```

### Múltiples Solicitudes de Prueba
```bash
# Movistar - 50 soles
curl -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"987654321","amount":50.0,"carrier":"MOVISTAR"}'

# Claro - 25 soles
curl -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"999888777","amount":25.0,"carrier":"CLARO"}'

# Entel - 15 soles
curl -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"955123456","amount":15.0,"carrier":"ENTEL"}'

# Claro - 500 soles - ERROR
curl -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"955123456","amount":500.0,"carrier":"CLARO"}'
```

---

## 📝 Script de Inicialización Completa

Para inicializar el sistema completo desde cero:

```bash
#!/bin/bash

# 1. Eliminar tablas existentes
echo "🗑️  Eliminando tablas existentes..."
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"DROP TABLE IF EXISTS process_audits; \
DROP TABLE IF EXISTS recharge_requests; \
DROP TABLE IF EXISTS balance_wallets;"

# 2. Crear tablas
echo "🏗️  Creando tablas..."
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"CREATE TABLE IF NOT EXISTS recharge_requests ( \
  recharge_id VARCHAR(36) PRIMARY KEY, \
  phone_number VARCHAR(15) NOT NULL, \
  amount DECIMAL(10,2) NOT NULL, \
  carrier VARCHAR(20), \
  status VARCHAR(20) DEFAULT 'PENDING', \
  created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6), \
  updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) \
); \
CREATE TABLE IF NOT EXISTS balance_wallets ( \
  operator_id INT AUTO_INCREMENT PRIMARY KEY, \
  operator_name VARCHAR(50) NOT NULL, \
  current_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00, \
  currency VARCHAR(3) DEFAULT 'PEN' \
); \
CREATE TABLE IF NOT EXISTS process_audits ( \
  audit_id INT AUTO_INCREMENT PRIMARY KEY, \
  recharge_id VARCHAR(36), \
  completion_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, \
  error_details TEXT, \
  CONSTRAINT fk_recharge FOREIGN KEY (recharge_id) REFERENCES recharge_requests(recharge_id) \
);"

# 3. Insertar saldo inicial
echo "💰 Insertando saldo inicial..."
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Movistar', 100.00, 'PEN'); \
INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Claro', 50.00, 'PEN');"

echo "✅ Sistema inicializado correctamente"
```

---
