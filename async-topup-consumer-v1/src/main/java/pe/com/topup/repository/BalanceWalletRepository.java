package pe.com.topup.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.entity.BalanceWallet;

@ApplicationScoped
public class BalanceWalletRepository implements PanacheRepositoryBase<BalanceWallet, Integer> {

    public Uni<BalanceWallet> findByOperatorName(String operatorName) {
        return find("operatorName", operatorName).firstResult();
    }
}
