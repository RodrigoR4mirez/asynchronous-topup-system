# async-topup-consumer-v1

## 📌 Descripción General

El **async-topup-consumer-v1** es el **componente más crítico del flujo de recargas**, ya que aquí ocurre la **lógica de negocio**, la **validación de saldos** y la **consistencia de datos**.

Este servicio funciona como un **worker asíncrono puro**, **sin endpoints REST**, y procesa eventos provenientes de **Kafka**, garantizando **atomicidad**, **auditoría** y **no bloqueo** usando programación reactiva.

---

## 🎯 Objetivo del Componente

Generar el componente **`topup-processor-worker`** utilizando:

- **Quarkus**
- **Hibernate Reactive con Mutiny**
- **Kafka con Avro**
- **MariaDB (reactivo)**

El worker se encarga de **procesar recargas telefónicas de forma asíncrona**, asegurando:

- Validación correcta de saldo
- Descuento atómico
- Actualización de estado
- Registro de auditoría

---

## 🗄️ Base de Datos

Base de datos: **`phone_recharge_db`**

### Tablas involucradas

#### 1️⃣ `recharge_requests`
- Actualizar el campo `status`
- Estados posibles:
  - `COMPLETED`
  - `FAILED`

#### 2️⃣ `balance_wallets`
- Validar:
  - `current_balance >= amount`
- Descontar saldo del operador

#### 3️⃣ `process_audits`
- Registrar:
  - Resultado final del proceso
  - Detalle del error si ocurre una falla

---

## 🔄 Flujo de Procesamiento

### Evento de Entrada

El worker recibe un **`TopUpEvent`** desde Kafka.

---

### Paso a Paso

#### 1️⃣ Recepción del Evento
- Kafka Consumer:
```text
@Incoming("topup-in")
```
- Deserialización **Avro**
- Conexión a **Schema Registry**

---

#### 2️⃣ Validación de Saldo
- Se consulta la tabla `balance_wallets`
- Se realiza el mapeo:
```text
TopUpEvent.carrier → balance_wallets.operator_name
```

---

#### 3️⃣ Decisión de Negocio

##### ✅ Si hay saldo suficiente
- Se descuenta el monto (`amount`) del `current_balance`
- Se actualiza `recharge_requests.status = COMPLETED`
- Se registra auditoría exitosa

##### ❌ Si NO hay saldo suficiente
- Se actualiza `recharge_requests.status = FAILED`
- Se registra auditoría con el mensaje:
```text
"Saldo insuficiente"
```

---

## 🔐 Atomicidad y Consistencia

Todo el flujo se ejecuta dentro de **una única transacción reactiva**, garantizando:

- No inconsistencias
- No estados intermedios inválidos
- Rollback automático ante errores

Se utiliza:
```text
@WithTransaction
```

Si el descuento de saldo falla, **el estado de la recarga NO cambia**.

---

## ⚡ Manejo de Concurrencia (Race Conditions)

Para evitar condiciones de carrera, el descuento de saldo se realiza con un **UPDATE directo**:

```sql
SET current_balance = current_balance - :amount
```

✔️ Operación atómica  
✔️ No bloqueante  
✔️ Segura bajo alta concurrencia  

---

## ✅ Características Clave

- ✔️ 100% Asíncrono
- ✔️ No Bloqueante
- ✔️ Transacciones reactivas
- ✔️ Kafka + Avro
- ✔️ Consistencia garantizada
- ✔️ Auditoría obligatoria

---

## 🏁 Resultado Final

Este worker es el **corazón del sistema de recargas**, asegurando que:

- Nunca se descuente saldo incorrectamente
- Nunca se complete una recarga sin validación
- Todo quede auditado
- El sistema sea escalable y resiliente

---

📌 **async-topup-consumer-v1**  
Worker crítico, reactivo y confiable.

---

## Tablas

```sql
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
                                                "CREATE TABLE IF NOT EXISTS recharge_requests ( \
                                                 recharge_id VARCHAR(36) PRIMARY KEY, \
                                                 phone_number VARCHAR(15) NOT NULL, \
                                                 amount DECIMAL(10,2) NOT NULL, \
                                                 status VARCHAR(20) DEFAULT 'PENDING', \
                                                 created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6), \
                                                 updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) ); \
                                                 \
                                                 CREATE TABLE IF NOT EXISTS balance_wallets ( \
                                                  operator_id INT AUTO_INCREMENT PRIMARY KEY, \
                                                  operator_name VARCHAR(50) NOT NULL, \
                                                  current_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00, \
                                                  currency VARCHAR(3) DEFAULT 'PEN' ); \
                                                \
                                                CREATE TABLE IF NOT EXISTS process_audits ( \
                                                  audit_id INT AUTO_INCREMENT PRIMARY KEY, \
                                                  recharge_id VARCHAR(36), \
                                                  completion_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, \
                                                  error_details TEXT, \
                                                  CONSTRAINT fk_recharge FOREIGN KEY (recharge_id) REFERENCES recharge_requests(recharge_id));"
```

```shell
## Eliminar topico
docker exec -it kafka-broker-1 kafka-topics \
  --bootstrap-server localhost:19092 \
  --delete --topic topup-topic
  
## crear topico de nuevo
docker exec -it kafka-broker-1 kafka-topics \
  --bootstrap-server localhost:19092 \
  --create --topic topup-topic \
  --partitions 3 --replication-factor 2
```

---

## 🐳 Docker

### Build Image
```bash
docker build -f Dockerfile -t async-topup-consumer-v1:latest .
```
**Explicación:** Construye la imagen Docker del consumer usando multi-stage build (Maven + OpenJDK 21).

### Run Container
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

**Explicación de variables de entorno:**
- `CONFIG_DB_HOST`: IP y puerto del host donde corre MariaDB (formato: host:puerto)
- `CONFIG_DB_USERNAME`: Usuario de base de datos
- `CONFIG_DB_PASSWORD`: Contraseña de base de datos
- `KAFKA_BROKERS`: Direcciones de los brokers de Kafka
- `SCHEMA_REGISTRY_URL`: URL del Schema Registry de Confluent

### Useful Commands

```bash
# Ver logs en tiempo real
docker logs -f async-consumer

# Ver logs de las últimas 100 líneas
docker logs --tail 100 async-consumer

# Buscar errores en logs
docker logs async-consumer 2>&1 | grep -i error

# Ver mensajes procesados de Kafka
docker logs async-consumer 2>&1 | grep -i "processing\|completed\|failed"

# Verificar conexión a Kafka
docker logs async-consumer 2>&1 | grep -i "SRMSG18257"

# Detener el contenedor
docker stop async-consumer

# Iniciar el contenedor
docker start async-consumer

# Reiniciar el contenedor
docker restart async-consumer

# Eliminar el contenedor
docker rm -f async-consumer

# Ver estado del contenedor
docker ps -a --filter "name=async-consumer"

# Entrar al contenedor (shell)
docker exec -it async-consumer /bin/bash
```

---

## 📊 Monitoring

### Verificar que el Consumer está procesando eventos
```bash
# Ver logs del consumer en tiempo real
docker logs -f async-consumer

# Verificar conexión a Kafka y subscripción al topic
docker logs async-consumer | grep "topup-topic"

# Ver transacciones completadas
docker logs async-consumer | grep "COMPLETED"

# Ver transacciones fallidas
docker logs async-consumer | grep "FAILED"
```

### Verificar procesamiento en base de datos
```bash
# Ver últimas recargas procesadas
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"SELECT recharge_id, phone_number, amount, status, created_at 
FROM recharge_requests 
ORDER BY created_at DESC LIMIT 10;"

# Ver saldos actuales
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"SELECT operator_name, current_balance FROM balance_wallets;"

# Ver auditoría
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"SELECT * FROM process_audits ORDER BY completion_date DESC LIMIT 5;"
```

---

## ⚠️ Troubleshooting

### El consumer no procesa mensajes
```bash
# 1. Verificar que Kafka esté accesible
telnet 192.168.18.29 19092

# 2. Verificar logs de conexión
docker logs async-consumer | grep -i "error\|exception"

# 3. Verificar que el topic existe
docker exec -it kafka-broker-1 kafka-topics --list --bootstrap-server localhost:19092

# 4. Ver mensajes en el topic
docker exec -it kafka-broker-1 kafka-console-consumer \
  --bootstrap-server localhost:19092 \
  --topic topup-topic --from-beginning
```

### Errores de base de datos
```bash
# Verificar conectividad a MariaDB
telnet 192.168.18.29 3307

# Ver logs específicos de BD
docker logs async-consumer 2>&1 | grep -i "mysql\|database\|connection"
```
