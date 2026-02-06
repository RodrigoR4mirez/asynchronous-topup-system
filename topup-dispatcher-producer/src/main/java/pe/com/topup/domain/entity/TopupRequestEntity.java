package pe.com.topup.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una solicitud de recarga en la base de datos.
 * <p>
 * Mapea la tabla "recharge_requests" y contiene la información fundamental
 * de la transacción que será procesada y enviada a Kafka.
 * </p>
 */
@Entity
@Table(name = "recharge_requests")
public class TopupRequestEntity {

    /**
     * Identificador único de la recarga.
     * <p>
     * Se define como clave primaria y tiene una longitud de 36 caracteres (UUID).
     * </p>
     */
    @Id
    @Column(name = "recharge_id", length = 36)
    public String rechargeId;

    /**
     * Número de teléfono beneficiario de la recarga.
     * <p>
     * Campo obligatorio con longitud máxima de 15 caracteres.
     * </p>
     */
    @Column(name = "phone_number", length = 15, nullable = false)
    public String phoneNumber;

    /**
     * Monto de la recarga.
     * <p>
     * Campo obligatorio definido con precisión 10 y escala 2 para valores
     * monetarios.
     * </p>
     */
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    public BigDecimal amount;

    /**
     * Estado actual de la solicitud (e.g., PENDING, SENT_TO_KAFKA).
     * <p>
     * Controla el flujo de procesamiento de la recarga.
     * </p>
     */
    @Column(name = "status", length = 20)
    public String status;

    /**
     * Fecha y hora de creación del registro.
     * <p>
     * Gestionado automáticamente por la base de datos (insertable=false,
     * updatable=false).
     * </p>
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    /**
     * Fecha y hora de la última actualización del registro.
     * <p>
     * Gestionado automáticamente por la base de datos (insertable=false,
     * updatable=false).
     * </p>
     */
    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
