package pe.com.topup.producer.domain.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import pe.com.topup.producer.domain.entity.TopupRequestEntity;

/**
 * Repositorio de acceso a datos para la entidad {@link TopupRequestEntity}.
 * <p>
 * Implementa {@link PanacheRepositoryBase} para proporcionar operaciones CRUD
 * reactivas
 * utilizando el patrón Repository de Panache.
 * </p>
 */
@ApplicationScoped
public class TopupRequestRepository implements PanacheRepositoryBase<TopupRequestEntity, String> {

    /**
     * Busca todas las solicitudes de recarga que se encuentran en estado 'PENDING'.
     * <p>
     * Paso 1: Realiza una consulta a la base de datos filtrando por el campo
     * 'status'.
     * Paso 2: Retorna una lista reactiva (Uni) con los resultados encontrados.
     * </p>
     *
     * @return Un {@link Uni} que emite una lista de {@link TopupRequestEntity} con
     *         estado 'PENDING'.
     */
    public Uni<List<TopupRequestEntity>> findPendingRequests() {
        return list("status", "PENDING");
    }
}
