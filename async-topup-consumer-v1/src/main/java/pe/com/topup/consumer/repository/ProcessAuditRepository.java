package pe.com.topup.consumer.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.topup.consumer.entity.ProcessAudit;

@ApplicationScoped
public class ProcessAuditRepository implements PanacheRepositoryBase<ProcessAudit, Integer> {
}
