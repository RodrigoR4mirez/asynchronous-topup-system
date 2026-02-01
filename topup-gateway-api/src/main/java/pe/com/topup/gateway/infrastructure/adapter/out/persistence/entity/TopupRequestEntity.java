package pe.com.topup.gateway.infrastructure.adapter.out.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Persistence entity mapping to TOPUP_REQUESTS table.
 */
@Entity
@Table(name = "recharge_requests")
public class TopupRequestEntity extends PanacheEntityBase {

    @Id
    @Column(name = "recharge_id", columnDefinition = "VARCHAR(36)")
    public String rechargeId;

    @Column(name = "phone_number", nullable = false, length = 15)
    public String phoneNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal amount;

    // Carrier is not in the provided schema, so we omit strict persistence or we
    // can add it if we think it's a mistake.
    // Given the strict instruction "la tabla que debe insertar es esta", we will
    // follow the schema.
    // However, the domain has carrier. We might lose data.
    // I'll stick to the provided schema columns strictly as requested.

    @Column(name = "status", columnDefinition = "ENUM('PENDING', 'PROCESSING', 'SUCCESSFUL', 'FAILED')")
    public String status;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", insertable = false, updatable = false)
    public java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", insertable = false, updatable = false)
    public java.time.LocalDateTime updatedAt;

    public TopupRequestEntity() {
    }
}
