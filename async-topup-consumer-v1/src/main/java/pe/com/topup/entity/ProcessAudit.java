package pe.com.topup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "process_audits")
public class ProcessAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    public Integer auditId;

    @Column(name = "recharge_id", length = 36)
    public String rechargeId;

    @CreationTimestamp
    @Column(name = "completion_date", columnDefinition = "TIMESTAMP")
    public LocalDateTime completionDate;

    @Column(name = "error_details", columnDefinition = "TEXT")
    public String errorDetails;

}
