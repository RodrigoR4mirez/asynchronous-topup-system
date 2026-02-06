package pe.com.topup.worker;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import pe.com.topup.entity.ProcessAudit;
import pe.com.topup.model.TopUpEvent;
import pe.com.topup.repository.BalanceWalletRepository;
import pe.com.topup.repository.ProcessAuditRepository;
import pe.com.topup.repository.RechargeRequestRepository;

@ApplicationScoped
public class TopupProcessor {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(TopupProcessor.class);

    @Inject
    BalanceWalletRepository balanceWalletRepository;

    @Inject
    RechargeRequestRepository rechargeRequestRepository;

    @Inject
    ProcessAuditRepository processAuditRepository;

    @Incoming("topup-consumer")
    public Uni<Void> process(TopUpEvent event) {
        if (event == null) {
            LOG.warn("Received null event, ignoring.");
            return Uni.createFrom().voidItem();
        }

        LOG.infof("Starting processing for RequestId: %s, Carrier: %s, Amount: %s",
                event.getRequestId(), event.getCarrier(), event.getAmount());

        BigDecimal amount = new BigDecimal(event.getAmount());
        String carrier = event.getCarrier();
        String requestId = event.getRequestId();

        LOG.debugf("Checking balance for Carrier: %s", carrier);

        return Panache.withTransaction(() -> balanceWalletRepository.findByOperatorName(carrier)
                .onItem().ifNotNull().transformToUni(wallet -> {
                    LOG.infof("Wallet found for carrier %s. Current Balance: %s", carrier, wallet.currentBalance);
                    if (wallet.currentBalance.compareTo(amount) >= 0) {
                        LOG.infof("Sufficient balance. Deducting %s from wallet.", amount);
                        // Saldo suficiente
                        return balanceWalletRepository
                                .update("currentBalance = currentBalance - ?1 where operatorId = ?2", amount,
                                        wallet.operatorId)
                                .chain(() -> {
                                    LOG.info("Wallet updated. Updating recharge status to COMPLETED.");
                                    return updateRechargeStatus(requestId, "COMPLETED");
                                })
                                .chain(() -> {
                                    LOG.info("Status updated. Creating success audit.");
                                    return createAudit(requestId, "Proceso exitoso");
                                });
                    } else {
                        LOG.warnf("Insufficient balance for RequestId: %s. Wallet Balance: %s, Required: %s",
                                requestId, wallet.currentBalance, amount);
                        // Saldo insuficiente
                        return updateRechargeStatus(requestId, "FAILED")
                                .chain(() -> {
                                    LOG.info("Status updated to FAILED. Creating failure audit.");
                                    return createAudit(requestId, "Saldo insuficiente");
                                });
                    }
                })
                // Si no se encuentra el operador (wallet es null)
                .onItem().ifNull().switchTo(() -> {
                    LOG.warnf("Operator not found for carrier: %s", carrier);
                    return updateRechargeStatus(requestId, "FAILED")
                            .chain(() -> createAudit(requestId, "Operador no encontrado: " + carrier));
                }))
                .replaceWithVoid()
                .onFailure().invoke(t -> LOG.errorf(t, "Error processing RequestId: %s", requestId))
                .onFailure().recoverWithUni(t ->
                // Error técnico dentro de la transacción o búsqueda
                Panache.withTransaction(() -> {
                    LOG.errorf("Recovering from error for RequestId: %s. Updating status to FAILED.", requestId);
                    return updateRechargeStatus(requestId, "FAILED")
                            .chain(() -> createAudit(requestId, "Error interno: " + t.getMessage()));
                }).replaceWithVoid());
    }

    private Uni<Integer> updateRechargeStatus(String requestId, String status) {
        LOG.debugf("Updating recharge request %s to status %s", requestId, status);
        return rechargeRequestRepository.update("status = ?1, updatedAt = ?2 where rechargeId = ?3",
                status, LocalDateTime.now(), requestId)
                .onItem().invoke(count -> LOG.debugf("Updated %d rows for recharge request %s", count, requestId));
    }

    private Uni<ProcessAudit> createAudit(String requestId, String details) {
        LOG.debugf("Creating audit for %s: %s", requestId, details);
        ProcessAudit audit = new ProcessAudit();
        audit.rechargeId = requestId;
        audit.errorDetails = details;
        return processAuditRepository.persist(audit)
                .onItem().invoke(a -> LOG.debugf("Audit created for %s", requestId));
    }
}
