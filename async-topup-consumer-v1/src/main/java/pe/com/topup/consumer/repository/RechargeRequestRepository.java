package pe.com.topup.consumer.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.consumer.entity.RechargeRequest;

@ApplicationScoped
public class RechargeRequestRepository implements PanacheRepositoryBase<RechargeRequest, String> {
}
