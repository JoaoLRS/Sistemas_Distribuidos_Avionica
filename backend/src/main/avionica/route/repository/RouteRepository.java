package avionica.route.repository;

import avionica.route.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

    List<Route> findAllByOrderByRegistradoEmDesc();

    Optional<Route> findFirstByCallsignAndIcaoOrigemAndIcaoDestinoOrderByRegistradoEmDesc(String callsign, String icaoOrigem, String icaoDestino);

    @Transactional
    @Modifying
    @Query("UPDATE Route r SET r.ativa = false WHERE r.callsign = ?1")
    void deactivatePreviousRoutes(String callsign);
}
