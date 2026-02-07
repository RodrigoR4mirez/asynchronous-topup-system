Actúa como un experto en Quarkus y Java 21. Genera el microservicio sync-topup-api-v1 utilizando un flujo 100% reactivo con Mutiny. El proyecto ya tiene configurada la conexión a la base de datos MariaDB en el archivo application.yml (o application.yaml), por lo que no es necesario generar archivos de configuración adicionales.

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

## 🧪 Testing
### CURL Test
```bash
response=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"985725003","amount":10,"carrier":"CLARO"}')
code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')
[ -n "$body" ] && echo "$body" | jq . 2>/dev/null || echo "✓ $code Accepted"
```

### Simple Test
```bash
curl -X POST http://localhost:8084/v1/topups \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"999888777","amount":50,"carrier":"MOVISTAR"}'
```

**Expected Response:** `HTTP 202 Accepted`


---

## 🐳 Docker

### Build Image
```bash
docker build -f Dockerfile -t sync-topup-api-v1:latest .
```
**Explicación:** Construye la imagen Docker del microservicio API usando multi-stage build (Maven + OpenJDK 21).

### Run Container
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

**Explicación de variables de entorno:**
- `DB_HOST`: IP del host donde corre MariaDB
- `DB_PORT`: Puerto de MariaDB (3307)
- `DB_USERNAME`: Usuario de base de datos
- `DB_PASSWORD`: Contraseña de base de datos

### Useful Commands

```bash
# Ver logs en tiempo real
docker logs -f sync-api

# Ver logs de las últimas 100 líneas
docker logs --tail 100 sync-api

# Detener el contenedor
docker stop sync-api

# Iniciar el contenedor
docker start sync-api

# Reiniciar el contenedor
docker restart sync-api

# Eliminar el contenedor
docker rm -f sync-api

# Ver estado del contenedor
docker ps -a --filter "name=sync-api"

# Entrar al contenedor (shell)
docker exec -it sync-api /bin/bash
```

---