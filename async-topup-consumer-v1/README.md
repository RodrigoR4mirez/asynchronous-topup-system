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
