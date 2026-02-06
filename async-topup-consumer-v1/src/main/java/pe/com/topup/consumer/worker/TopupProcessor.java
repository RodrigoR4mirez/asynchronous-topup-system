package pe.com.topup.consumer.worker;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import pe.com.topup.consumer.entity.ProcessAudit;
import pe.com.topup.consumer.repository.BalanceWalletRepository;
import pe.com.topup.consumer.repository.ProcessAuditRepository;
import pe.com.topup.consumer.repository.RechargeRequestRepository;
import pe.com.topup.model.TopUpEvent;

@ApplicationScoped
public class TopupProcessor {

    @Inject
    BalanceWalletRepository balanceWalletRepository;

    @Inject
    RechargeRequestRepository rechargeRequestRepository;

    @Inject
    ProcessAuditRepository processAuditRepository;

    @Incoming("topup-consumer")
    public Uni<Void> process(TopUpEvent event) {
        if (event == null) {
            return Uni.createFrom().voidItem();
        }

        BigDecimal amount = new BigDecimal(event.getAmount());
        String carrier = event.getCarrier();
        String requestId = event.getRequestId();

        return Panache.withTransaction(() -> balanceWalletRepository.findByOperatorName(carrier)
                .onItem().ifNotNull().transformToUni(wallet -> {
                    if (wallet.currentBalance.compareTo(amount) >= 0) {
                        // Saldo suficiente
                        return balanceWalletRepository
                                .update("currentBalance = currentBalance - ?1 where operatorId = ?2", amount,
                                        wallet.operatorId)
                                .chain(() -> updateRechargeStatus(requestId, "COMPLETED"))
                                .chain(() -> createAudit(requestId, "Proceso exitoso"));
                    } else {
                        // Saldo insuficiente
                        return updateRechargeStatus(requestId, "FAILED")
                                .chain(() -> createAudit(requestId, "Saldo insuficiente"));
                    }
                })
                // Si no se encuentra el operador (wallet es null)
                .onItem().ifNull().switchTo(() -> updateRechargeStatus(requestId, "FAILED")
                        .chain(() -> createAudit(requestId, "Operador no encontrado: " + carrier))))
                .replaceWithVoid().onFailure().recoverWithUni(t ->
                // Error técnico dentro de la transacción o búsqueda
                Panache.withTransaction(() -> updateRechargeStatus(requestId, "FAILED")
                        .chain(() -> createAudit(requestId, "Error interno: " + t.getMessage()))).replaceWithVoid());
    }

    private Uni<Integer> updateRechargeStatus(String requestId, String status) {
        return rechargeRequestRepository.update("status = ?1, updatedAt = ?2 where rechargeId = ?3",
                status, LocalDateTime.now(), requestId);
    }

    private Uni<ProcessAudit> createAudit(String requestId, String details) {
        ProcessAudit audit = new ProcessAudit();
        audit.rechargeId = requestId;
        audit.errorDetails = details;
        return processAuditRepository.persist(audit);
    }
}
