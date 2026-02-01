package pe.com.topup.gateway.domain.port;

import io.smallrye.mutiny.Uni;
import pe.com.topup.gateway.domain.model.TopupRequest;

/**
 * Output port for saving TopupRequests.
 * This interface decouples the domain from the persistence implementation.
 */
public interface TopupRepositoryPort {
    /**
     * Persists a topup request.
     * 
     * @param topupRequest the domain entity to save
     * @return a Uni containing the saved entity (potentially with ID populated or
     *         internal updates)
     */
    Uni<TopupRequest> save(TopupRequest topupRequest);
}
