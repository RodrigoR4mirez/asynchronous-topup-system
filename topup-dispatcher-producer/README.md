Actúa como un desarrollador senior de Quarkus 3 y Java 21. Debes generar el componente topup-dispatcher-producer. Este es un Worker Asíncrono encargado de desacoplar la API de la mensajería mediante el envío de eventos a Kafka. La conexión a MariaDB ya está configurada en el application.yml y las dependencias de Kafka (SmallRye Reactive Messaging) ya están en el pom.xml.

1. Entidad de Referencia: Utiliza la siguiente entidad existente para todas las operaciones de base de datos:

Java
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

SQL
CREATE TABLE recharge_requests (
    recharge_id VARCHAR(36) PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

2. Lógica de Activación y Polling:

Implementa una tarea programada con @Scheduled(every = "1s").

El flujo debe ser 100% reactivo usando Mutiny.

Proceso:

Consultar en MariaDB los registros con status = 'PENDING'.

Transformar cada registro al esquema de evento de Kafka.

Enviar el mensaje al tópico topic-validacion-recarga de forma asíncrona.

Si el envío es exitoso, actualizar el estado del registro a SENT_TO_KAFKA.

3. Documentación Javadoc (En Español):

Obligatorio: Documenta cada clase y método en español explicando su funcionamiento paso a paso:

Paso 1: Inicio del ciclo de escaneo (Polling) cada segundo.

Paso 2: Recuperación reactiva de solicitudes pendientes desde la tabla recharge_requests.

Paso 3: Serialización de la información al formato de mensajería asíncrona.

Paso 4: Publicación del evento en el broker de Kafka.

Paso 5: Actualización final del estado en la base de datos para confirmar el envío.

4. Robustez:

Si Kafka no está disponible o el envío falla, el registro debe permanecer en PENDING para ser reintentado en el siguiente ciclo.

Asegura que el uso de los canales de Kafka coincida con la configuración estándar de Quarkus.

Restricciones:

No generes archivos OpenAPI/YAML ni tests unitarios.

No uses arquitectura hexagonal; mantén una estructura de paquetes estándar y limpia.

Asegúrate de validar que el componente funcione de forma no bloqueante.


---
# Crear el nuevo tópico para recargas con dos particiones

docker exec -it kafka-broker-1 kafka-topics --bootstrap-server kafka-broker-1:9092,kafka-broker-2:9092 --if-not-exists --create --topic topup-topic --partitions 2 --replication-factor 2


--
# ELIMINAR TOPICO
docker exec -it kafka-broker-1 kafka-topics \
  --bootstrap-server localhost:19092 \
  --delete \
  --topic topup-topic

--
## PRODUCER
docker exec -it kafka-broker-1 kafka-console-producer --bootstrap-server kafka-broker-1:9092 --topic topup-topic

--
## CONSUMER
docker exec -it kafka-broker-1 kafka-console-consumer --bootstrap-server kafka-broker-1:9092 --topic topup-topic --from-beginning

--
# AVRO: 
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
### JSON
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "phoneNumber": "987654321",
  "amount": 50.0,
  "carrier": "MOVISTAR"
}