package pe.com.topup.consumer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "balance_wallets")
public class BalanceWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operator_id")
    public Integer operatorId;

    @Column(name = "operator_name", nullable = false, length = 50)
    public String operatorName;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    public BigDecimal currentBalance;

    @Column(length = 3)
    public String currency;
}
