package pe.com.topup.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.entity.RechargeRequest;

@ApplicationScoped
public class RechargeRequestRepository implements PanacheRepositoryBase<RechargeRequest, String> {
}
