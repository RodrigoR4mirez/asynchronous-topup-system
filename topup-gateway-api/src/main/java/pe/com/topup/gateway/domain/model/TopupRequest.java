package pe.com.topup.gateway.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain entity representing a Topup Request.
 */
public class TopupRequest {
    private UUID id;
    private String phoneNumber;
    private BigDecimal amount;
    private String carrier;
    private String status;

    public TopupRequest() {
    }

    public TopupRequest(UUID id, String phoneNumber, BigDecimal amount, String carrier, String status) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.amount = amount;
        this.carrier = carrier;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
