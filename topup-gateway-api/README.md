Actúa como un experto en Quarkus y Java 21. Genera el microservicio topup-gateway-api utilizando un flujo 100% reactivo con Mutiny. El proyecto ya tiene configurada la conexión a la base de datos MariaDB en el archivo application.yml (o application.yaml), por lo que no es necesario generar archivos de configuración adicionales.

Instrucciones del Proyecto:

Modelos de Datos (DTOs):

Crea un TopupRequest con: phoneNumber (String), amount (BigDecimal) y carrier (Enum: MOVISTAR, CLARO, ENTEL).

Crea un ErrorResponse con: code (String), message (String) y details (List de Strings).

Usa anotaciones de Bean Validation (@NotBlank, @Positive, @NotNull) para validar la entrada.

Entidad y Base de Datos:

Define la entidad TopupRequestEntity usando Panache Entity (Reactive).

Debe mapear a la tabla recharge_requests con esta estructura:

SQL
CREATE TABLE recharge_requests (
    recharge_id VARCHAR(36) PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
Configuración Crítica: Para evitar errores de validación de Hibernate, mapea status como String. Las columnas created_at y updated_at deben tener insertable = false, updatable = false para que MariaDB gestione sus valores automáticamente.

Endpoint y Lógica:

Crea un recurso REST en POST /v1/topups.

La lógica debe ser reactiva:

Validar el DTO de entrada.

Generar un UUID para el recharge_id.

Persistir la entidad con estado PENDING.

Retornar un 202 Accepted tras la persistencia exitosa.

Manejo de Errores:

Implementa un ExceptionMapper reactivo que capture ConstraintViolationException.

Debe devolver un 400 Bad Request con el formato de ErrorResponse definido, detallando los campos inválidos.

Restricciones:

No generes archivos OpenAPI/YAML.

No uses arquitectura hexagonal; mantén una estructura de paquetes estándar.

No generes tests unitarios.

Documenta cada método y clase con Javadoc.

## 🗄️ Diccionario de Datos: `recharge_requests`

| Campo | Tipo | ¿Para qué sirve? | ¿Cuándo cambia o se asigna? |
| --- | --- | --- | --- |
| **`recharge_id`** | `VARCHAR(36)` | Identificador único universal (UUID) de la transacción. | Se genera automáticamente en la capa de aplicación al recibir un nuevo `POST` exitoso. Es inmutable. |
| **`phone_number`** | `VARCHAR(15)` | Número celular destino de la recarga. | Se asigna en la creación según el JSON de entrada. No debe cambiar tras la inserción. |
| **`amount`** | `DECIMAL` | Monto monetario que se desea recargar. | Se define en la creación. Debe validarse como un valor positivo antes de persistirse. |
| **`status`** | `ENUM` | Representa el estado actual de la recarga dentro del flujo asíncrono. | Cambia a lo largo del flujo: `PENDING` al crear, `PROCESSING` cuando es tomada por el dispatcher, y `SUCCESSFUL` o `FAILED` tras la validación final. |
| **`created_at`** | `TIMESTAMP` | Fecha y hora en que se recibió la solicitud. | Se asigna automáticamente al insertar el registro mediante `DEFAULT CURRENT_TIMESTAMP`. |
| **`updated_at`** | `TIMESTAMP` | Fecha y hora de la última actualización del registro. | Se actualiza automáticamente cada vez que el estado u otro campo del registro cambia. |


## 📝 Detalle del Request Body (JSON)

| Campo | Tipo | Requisito | Validación / Descripción |
| --- | --- | --- | --- |
| `phoneNumber` | `String` | **Obligatorio** | Número de teléfono destino de la recarga. Debe ser una cadena no vacía; se recomienda usar formato internacional. |
| `amount` | `Decimal` | **Obligatorio** | Monto monetario de la recarga. Debe ser un valor positivo, con un mínimo permitido de `0.1`. |
| `carrier` | `Enum` | **Obligatorio** | Operadora telefónica asociada al número. Valores permitidos: `MOVISTAR`, `CLARO`, `ENTEL`. |