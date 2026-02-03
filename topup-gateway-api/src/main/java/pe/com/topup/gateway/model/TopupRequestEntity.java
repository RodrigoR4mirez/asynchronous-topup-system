package pe.com.topup.gateway.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a Topup Request in the database.
 * Mapped to table "recharge_requests".
 */
@Entity
@Table(name = "recharge_requests")
public class TopupRequestEntity extends PanacheEntityBase {

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
    @CreationTimestamp
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @UpdateTimestamp
    public LocalDateTime updatedAt;

    /**
     * Default constructor required by Hibernate.
     */
    public TopupRequestEntity() {
    }

    /**
     * Constructor for creating a new TopupRequestEntity.
     * 
     * @param rechargeId  The UUID of the recharge.
     * @param phoneNumber The phone number.
     * @param amount      The amount.
     * @param status      The initial status.
     */
    public TopupRequestEntity(String rechargeId, String phoneNumber, BigDecimal amount, String status) {
        this.rechargeId = rechargeId;
        this.phoneNumber = phoneNumber;
        this.amount = amount;
        this.status = status;
    }
}
