package pe.com.topup.gateway.resource;

import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.com.topup.gateway.dto.TopupRequest;
import pe.com.topup.gateway.service.TopupService;
import jakarta.inject.Inject;

/**
 * REST Resource for managing Topup operations.
 */
@Path("/v1/topups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TopupResource {

    private final TopupService topupService;

    @Inject
    public TopupResource(TopupService topupService) {
        this.topupService = topupService;
    }

    /**
     * Creates a new topup request.
     * Delegates to TopupService.
     *
     * @param request The topup request DTO.
     * @return A Uni resolving to the Response.
     */
    @POST
    public Uni<Response> createTopup(@Valid TopupRequest request) {
        return topupService.registerTopup(request)
                .map(v -> Response.accepted().build());
    }
}
