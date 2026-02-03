package pe.com.topup.gateway.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.gateway.dto.TopupRequest;
import pe.com.topup.gateway.model.TopupRequestEntity;

import java.util.UUID;

/**
 * Mapper for converting between DTOs and Entities.
 * Follows Single Responsibility Principle by isolating mapping logic.
 */
@ApplicationScoped
public class TopupMapper {

    public TopupRequestEntity toEntity(TopupRequest request) {
        if (request == null) {
            return null;
        }
        String rechargeId = UUID.randomUUID().toString();
        return new TopupRequestEntity(
                rechargeId,
                request.getPhoneNumber(),
                request.getAmount(),
                "PENDING");
    }
}
