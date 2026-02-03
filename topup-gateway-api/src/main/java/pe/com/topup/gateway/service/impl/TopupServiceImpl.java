package pe.com.topup.gateway.service.impl;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pe.com.topup.gateway.dto.TopupRequest;
import pe.com.topup.gateway.infrastructure.adapter.out.persistence.entity.TopupRequestEntity;
import pe.com.topup.gateway.mapper.TopupMapper;
import pe.com.topup.gateway.repository.TopupRepository;
import pe.com.topup.gateway.service.TopupService;

/**
 * Implementation of TopupService.
 * Orchestrates validation, mapping, and persistence.
 */
@ApplicationScoped
public class TopupServiceImpl implements TopupService {

    private final TopupRepository repository;
    private final TopupMapper mapper;

    @Inject
    public TopupServiceImpl(TopupRepository repository, TopupMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @WithTransaction
    public Uni<Void> registerTopup(TopupRequest request) {
        TopupRequestEntity entity = mapper.toEntity(request);
        return repository.persist(entity)
                .replaceWithVoid();
    }
}
