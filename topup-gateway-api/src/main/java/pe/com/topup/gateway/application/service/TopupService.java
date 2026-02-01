package pe.com.topup.gateway.application.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pe.com.topup.gateway.domain.model.TopupRequest;
import pe.com.topup.gateway.domain.port.TopupRepositoryPort;

import java.util.UUID;

@ApplicationScoped
public class TopupService {

    private final TopupRepositoryPort repository;

    protected TopupService() {
        this.repository = null;
    }

    @Inject
    public TopupService(TopupRepositoryPort repository) {
        this.repository = repository;
    }

    public Uni<TopupRequest> processTopup(TopupRequest request) {
        // Business Logic: Generate ID and set status
        request.setId(UUID.randomUUID());
        request.setStatus("ACCEPTED"); // As per 202 requirement, we accept it.

        return repository.save(request);
    }
}
