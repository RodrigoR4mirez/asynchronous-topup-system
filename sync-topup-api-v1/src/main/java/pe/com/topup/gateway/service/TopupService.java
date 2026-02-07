package pe.com.topup.gateway.service;

import io.smallrye.mutiny.Uni;
import pe.com.topup.gateway.dto.TopupRequest;

/**
 * Service Interface for Topup operations.
 * Defines the business logic contract.
 */
public interface TopupService {

    /**
     * Processes and stores a new topup request.
     * 
     * @param request The topup request DTO.
     * @return A Uni that completes when the operation is finished.
     */
    Uni<Void> registerTopup(TopupRequest request);
}
