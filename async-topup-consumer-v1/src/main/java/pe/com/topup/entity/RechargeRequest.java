package pe.com.topup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "recharge_requests")
public class RechargeRequest {

    @Id
    @Column(name = "recharge_id", length = 36)
    public String rechargeId;

    @Column(name = "phone_number", nullable = false, length = 15)
    public String phoneNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal amount;

    @Column(length = 20)
    public String status;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

}
