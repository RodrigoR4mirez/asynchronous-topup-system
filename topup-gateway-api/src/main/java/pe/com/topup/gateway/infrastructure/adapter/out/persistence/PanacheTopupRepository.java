package pe.com.topup.gateway.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.gateway.domain.model.TopupRequest;
import pe.com.topup.gateway.domain.port.TopupRepositoryPort;
import pe.com.topup.gateway.infrastructure.adapter.out.persistence.entity.TopupRequestEntity;

@ApplicationScoped
public class PanacheTopupRepository implements TopupRepositoryPort {

    @Override
    public Uni<TopupRequest> save(TopupRequest domain) {
        TopupRequestEntity entity = toEntity(domain);
        return Panache.withTransaction(entity::persist)
                .map(e -> toDomain((TopupRequestEntity) e));
    }

    private TopupRequestEntity toEntity(TopupRequest domain) {
        TopupRequestEntity entity = new TopupRequestEntity();
        if (domain.getId() != null) {
            entity.rechargeId = domain.getId().toString();
        }
        entity.phoneNumber = domain.getPhoneNumber();
        entity.amount = domain.getAmount();
        // Carrier not persisted in this simplified schema
        entity.status = domain.getStatus();
        return entity;
    }

    private TopupRequest toDomain(TopupRequestEntity entity) {
        java.util.UUID id = entity.rechargeId != null ? java.util.UUID.fromString(entity.rechargeId) : null;
        return new TopupRequest(
                id,
                entity.phoneNumber,
                entity.amount,
                "UNKNOWN", // Carrier lost in persistence
                entity.status);
    }
}
