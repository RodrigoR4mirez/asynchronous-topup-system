package pe.com.topup.gateway.infrastructure.adapter.in.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import pe.com.topup.gateway.application.service.TopupService;
import pe.com.topup.gateway.domain.model.TopupRequest;
import pe.com.topup.gateway.infrastructure.adapter.in.web.api.V1Api;
import pe.com.topup.gateway.infrastructure.adapter.in.web.dto.TopupRequestDto;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Adapter In (REST) implementing the generated API interface.
 * Decouples the API contract (generated) from the Domain.
 */
@ApplicationScoped
public class TopupResourceImpl implements V1Api {

    private final TopupService topupService;

    protected TopupResourceImpl() {
        this.topupService = null;
    }

    @Inject
    public TopupResourceImpl(TopupService topupService) {
        this.topupService = topupService;
    }

    @Override
    public java.util.concurrent.CompletionStage<Response> requestTopup(TopupRequestDto requestDto) {
        TopupRequest domain = toDomain(requestDto);
        return topupService.processTopup(domain)
                .map(saved -> Response.accepted().build())
                .subscribe().asCompletionStage();
    }

    private TopupRequest toDomain(TopupRequestDto dto) {
        // Mapping generated DTO to Domain Entity
        TopupRequest domain = new TopupRequest();

        domain.setPhoneNumber(dto.getPhoneNumber());
        // Generated DTO uses Double for 'number' format 'double'
        if (dto.getAmount() != null) {
            domain.setAmount(java.math.BigDecimal.valueOf(dto.getAmount()));
        }
        if (dto.getCarrier() != null) {
            domain.setCarrier(dto.getCarrier().name());
        }
        return domain;
    }
}
