# Topup Dispatcher Producer

Actúa como un desarrollador senior de **Quarkus 3** y **Java 21**.  
Debes generar el componente **topup-dispatcher-producer**.

Este es un **Worker Asíncrono** encargado de desacoplar la API de la mensajería mediante el envío de eventos a **Kafka**.  
La conexión a **MariaDB** ya está configurada en el `application.yml` y las dependencias de Kafka (**SmallRye Reactive Messaging**) ya están en el `pom.xml`.

---

## 1. Entidad de Referencia

Utiliza la siguiente entidad existente para todas las operaciones de base de datos:

### Java
```java
@Entity
@Table(name = "recharge_requests")
public class TopupRequestEntity {
    @Id
    @Column(name = "recharge_id", length = 36)
    public String rechargeId;

    @Column(name = "phone_number", length = 15, nullable = false)
    public String phoneNumber;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    public BigDecimal amount;

    @Column(name = "status", length = 20)
    public String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
```

### SQL
```sql
CREATE TABLE recharge_requests (
    recharge_id VARCHAR(36) PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 2. Lógica de Activación y Polling

Implementa una tarea programada con:

```java
@Scheduled(every = "1s")
```

El flujo debe ser **100% reactivo** usando **Mutiny**.

### Proceso
1. Consultar en MariaDB los registros con `status = 'PENDING'`.
2. Transformar cada registro al esquema de evento de Kafka.
3. Enviar el mensaje al tópico `topic-validacion-recarga` de forma asíncrona.
4. Si el envío es exitoso, actualizar el estado del registro a `SENT_TO_KAFKA`.

---

## 3. Documentación Javadoc (En Español)

**Obligatorio**: Documenta cada clase y método en español explicando su funcionamiento paso a paso:

- **Paso 1**: Inicio del ciclo de escaneo (Polling) cada segundo.
- **Paso 2**: Recuperación reactiva de solicitudes pendientes desde la tabla `recharge_requests`.
- **Paso 3**: Serialización de la información al formato de mensajería asíncrona.
- **Paso 4**: Publicación del evento en el broker de Kafka.
- **Paso 5**: Actualización final del estado en la base de datos para confirmar el envío.

---

## 4. Robustez

- Si Kafka no está disponible o el envío falla, el registro debe permanecer en `PENDING` para ser reintentado en el siguiente ciclo.
- Asegura que el uso de los canales de Kafka coincida con la configuración estándar de Quarkus.

---

## Restricciones

- No generes archivos OpenAPI/YAML ni tests unitarios.
- No uses arquitectura hexagonal; mantén una estructura de paquetes estándar y limpia.
- Asegúrate de validar que el componente funcione de forma no bloqueante.

---

## Crear el nuevo tópico para recargas con dos particiones

```bash
docker exec -it kafka-broker-1 kafka-topics   --bootstrap-server kafka-broker-1:9092,kafka-broker-2:9092   --if-not-exists   --create   --topic topup-topic   --partitions 2   --replication-factor 2
```

---

## Eliminar tópico

```bash
docker exec -it kafka-broker-1 kafka-topics   --bootstrap-server localhost:19092   --delete   --topic topup-topic
```

---

## Producer

```bash
docker exec -it kafka-broker-1 kafka-console-producer   --bootstrap-server kafka-broker-1:9092   --topic topup-topic
```

---

## Consumer

```bash
docker exec -it kafka-broker-1 kafka-console-consumer   --bootstrap-server kafka-broker-1:9092   --topic topup-topic   --from-beginning
```

---

## AVRO

```json
{
  "type": "record",
  "name": "TopUpEvent",
  "namespace": "pe.com.topup.producer.model",
  "fields": [
    { "name": "requestId", "type": ["null", "string"], "default": null },
    { "name": "phoneNumber", "type": ["null", "string"], "default": null },
    { "name": "amount", "type": ["null", "double"], "default": null },
    { "name": "carrier", "type": ["null", "string"], "default": null }
  ]
}
```

### JSON
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "phoneNumber": "987654321",
  "amount": 50.0,
  "carrier": "MOVISTAR"
}
```

---
---
---
---
---


# Topup Dispatcher Producer

Componente encargado de procesar solicitudes de recarga pendientes de forma asíncrona. Actúa como un *Worker* reactivo que consulta la base de datos y publica eventos en Kafka.

## Arquitectura

Este microservicio utiliza **Quarkus 3** y **Mutiny** para un procesamiento no bloqueante.
1. **Polling**: Un `@Scheduled` consulta registros con estado `PENDING` en MariaDB cada segundo.
2. **Processing**: Transforma la entidad JPA a un evento.
3. **Publishing**: Envía el evento al tópico `topup-topic` en Kafka.
4. **Completion**: Actualiza el estado del registro en la base de datos a `SENT_TO_KAFKA`.

## Prerrequisitos

Asegúrate de tener la infraestructura de contenedores ejecutándose (Kafka, Zookeeper, MariaDB).

```bash
docker ps
# Debes ver: kafka-broker-1, mariadb10432, etc.
```

## Ejecución

Para iniciar la aplicación en modo desarrollo:

```bash
./mvnw quarkus:dev
```

La aplicación iniciará en el puerto **9091** (configurado en `application.yml`).

## Guía de Pruebas (Paso a Paso)

Sigue estos pasos para validar el flujo completo "End-to-End".

### 1. Preparar el Consumidor de Kafka
Abre una terminal nueva y ejecuta este comando para "escuchar" los mensajes que llegarán al tópico. Mantén esta terminal visible.

```bash
docker exec -it kafka-broker-1 kafka-console-consumer \
  --bootstrap-server kafka-broker-1:9092 \
  --topic topup-topic \
  --from-beginning
```

### 2. Insertar una Solicitud "PENDING"
Simularemos una petición de recarga insertando un registro directamente en la base de datos.
Abre otra terminal y ejecuta:

```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "
INSERT INTO recharge_requests (recharge_id, phone_number, amount, status, created_at)
VALUES (UUID(), '999888777', 50.00, 'PENDING', NOW());
"
```

### 3. Verificar el Procesamiento

**En la terminal de la aplicación (Quarkus Logs):**
Deberías ver logs indicando el procesamiento:
> INFO  [pe.com.top.pro.app.ser.DispatcherService] Paso 2: Se encontraron 1 solicitudes pendientes.
> INFO  [pe.com.top.pro.app.ser.DispatcherService] Evento enviado a Kafka para ID: ...
> INFO  [pe.com.top.pro.app.ser.DispatcherService] Estado actualizado a SENT_TO_KAFKA para ID: ...

**En la terminal del Consumidor Kafka (Paso 1):**
Debería aparecer el mensaje JSON:
```json
{"rechargeId":"...","phoneNumber":"999888777","amount":50.0,"status":"PENDING"}
```

### 4. Verificar el Estado Final en Base de Datos
Consulta la base de datos para asegurar que el estado cambió de `PENDING` a `SENT_TO_KAFKA`.

```bash
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "
SELECT recharge_id, phone_number, status FROM recharge_requests WHERE status = 'SENT_TO_KAFKA';
"
```

## Solución de Problemas

- **Si no conecta a Kafka**: Verifica que `kafka-broker-1` sea accesible en `localhost:19092` (puerto externo configurado en docker-compose).
- **Si no conecta a la BD**: Verifica que `mariadb10432` esté corriendo en el puerto `3307` y las credenciales en `application.yml` sean correctas.

## Consulta a bd
```bash

docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"drop table balance_wallets; \
drop table process_audits; \
drop table recharge_requests;"

docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e \
"SELECT * FROM balance_wallets; \
SELECT * FROM process_audits; \
SELECT * FROM recharge_requests;"

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

