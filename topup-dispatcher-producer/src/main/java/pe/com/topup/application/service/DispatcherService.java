package pe.com.topup.application.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import org.eclipse.microprofile.reactive.messaging.Channel;
import pe.com.topup.domain.repository.TopupRequestRepository;
import pe.com.topup.model.TopUpEvent;

import java.util.logging.Logger;

/**
 * Servicio encargado de orquestar el envío de solicitudes de recarga pendientes
 * a Kafka.
 * <p>
 * Este componente actúa como un 'Dispatcher' que consulta periódicamente la
 * base de datos
 * y envía los registros pendientes al sistema de mensajería para su
 * procesamiento asíncrono.
 * </p>
 */
@ApplicationScoped
public class DispatcherService {

        private static final Logger LOG = Logger.getLogger(DispatcherService.class.getName());

        @Inject
        TopupRequestRepository repository;

        @Inject
        @Channel("topup-requests")
        MutinyEmitter<TopUpEvent> emitter;

        /**
         * Tarea programada que se ejecuta cada segundo para procesar solicitudes
         * pendientes.
         * <p>
         * El flujo reactivo es el siguiente:
         * 1. Consulta la base de datos buscando registros en estado 'PENDING'.
         * 2. Transforma cada entidad en un evento {@link TopUpEvent}.
         * 3. Envía el evento a Kafka de forma asíncrona.
         * 4. Si el envío es exitoso, actualiza el estado en base de datos a
         * 'SENT_TO_KAFKA'.
         * 5. Si ocurre un error en el envío, el registro permanece en 'PENDING' para
         * reintento.
         * </p>
         *
         * @return Un {@link Uni} que representa la completitud del ciclo de
         *         procesamiento.
         */
        @Scheduled(every = "10s")
        @WithTransaction
        public Uni<Void> processPendingRequests() {
                LOG.info("Paso 1: Inicio del ciclo de escaneo (Polling) de solicitudes pendientes.");

                return repository.findPendingRequests()
                                .onItem().transformToMulti(list -> {
                                        if (list.isEmpty()) {
                                                return Multi.createFrom().empty();
                                        }
                                        LOG.info("Paso 2: Se encontraron " + list.size()
                                                        + " solicitudes pendientes. IDs: "
                                                        + list.stream().map(e -> e.rechargeId).toList());
                                        return Multi.createFrom().iterable(list);
                                })
                                .onItem().transformToUniAndConcatenate(entity -> {
                                        LOG.info("Paso 3: Procesando ID " + entity.rechargeId
                                                        + ". Transformando a evento Avro. Datos: "
                                                        + entity.toString());

                                        // Paso 3: Transformación a objeto Avro generado.
                                        TopUpEvent event = TopUpEvent.newBuilder()
                                                        .setRequestId(entity.rechargeId)
                                                        .setPhoneNumber(entity.phoneNumber)
                                                        .setAmount(entity.amount != null ? entity.amount.doubleValue()
                                                                        : null)
                                                        .setCarrier(null) // El entity no tiene carrier, se envía null
                                                        .build();

                                        LOG.info("Paso 4: Evento Avro creado. Payload: " + event.toString()
                                                        + ". Enviando a Kafka...");
                                        // Paso 4: Publicación del evento en el broker de Kafka.
                                        return emitter.send(event)
                                                        .onItem()
                                                        .invoke(() -> LOG
                                                                        .info("Paso 5: Evento enviado exitosamente a Kafka para ID: "
                                                                                        + entity.rechargeId))
                                                        .onItem().transformToUni(v -> {
                                                                // Paso 5: Actualización final del estado en la base de
                                                                // datos para confirmar el
                                                                // envío.
                                                                return repository.update(
                                                                                "status = 'SENT_TO_KAFKA' where rechargeId = ?1",
                                                                                entity.rechargeId)
                                                                                .onItem().invoke(() -> LOG.info(
                                                                                                "Paso 6: Estado en DB actualizado a SENT_TO_KAFKA para ID: "
                                                                                                                + entity.rechargeId));
                                                        })
                                                        .onFailure()
                                                        .invoke(ex -> LOG
                                                                        .severe("Error en el flujo para ID "
                                                                                        + entity.rechargeId + ": "
                                                                                        + ex.getMessage()))
                                                        // Robustez: Si falla, recuperamos con null para no romper el
                                                        // flujo de otros
                                                        // items
                                                        .onFailure().recoverWithNull();
                                })
                                .collect().asList()
                                .replaceWithVoid();
        }
}
