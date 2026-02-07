package pe.com.topup.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.entity.BalanceWallet;

@ApplicationScoped
public class BalanceWalletRepository implements PanacheRepositoryBase<BalanceWallet, Integer> {

    public Uni<BalanceWallet> findByOperatorName(String operatorName) {
        // Case-insensitive search to handle MOVISTAR vs Movistar
        return find("LOWER(operatorName) = LOWER(?1)", operatorName).firstResult();
    }
}
