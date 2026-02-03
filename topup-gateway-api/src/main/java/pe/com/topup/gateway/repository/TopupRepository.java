package pe.com.topup.gateway.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.gateway.model.TopupRequestEntity;

/**
 * Repository for TopupRequestEntity.
 * Implements the Repository Pattern using Panache.
 */
@ApplicationScoped
public class TopupRepository implements PanacheRepositoryBase<TopupRequestEntity, String> {
}
