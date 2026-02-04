package pe.com.topup.producer.application.dto;

import java.math.BigDecimal;

/**
 * Objeto de Transferencia de Datos (DTO) que representa el evento de solicitud
 * de recarga.
 * <p>
 * Este objeto se serializa y envía al tópico de Kafka
 * 'topic-validacion-recarga'.
 * Contiene la información necesaria para que los consumidores procesen la
 * recarga.
 * </p>
 */
public record TopupRequestEvent(
        /**
         * Identificador único de la recarga.
         */
        String rechargeId,

        /**
         * Número de teléfono beneficiario.
         */
        String phoneNumber,

        /**
         * Monto de la recarga.
         */
        BigDecimal amount,

        /**
         * Estado de la recarga al momento del envío.
         */
        String status) {
}
