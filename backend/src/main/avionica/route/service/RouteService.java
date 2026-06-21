package avionica.route.service;

import avionica.kafka.RouteKafkaProducer;
import avionica.route.dto.RouteRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RouteService {

    private final JdbcTemplate jdbc;
    private final RouteKafkaProducer kafkaProducer;

    public RouteService(JdbcTemplate jdbc, RouteKafkaProducer kafkaProducer) {
        this.jdbc = jdbc;
        this.kafkaProducer = kafkaProducer;
    }

    public void requestRoute(RouteRequest request) {
        String callsign   = request.callsign().trim().toUpperCase();
        String origin     = request.origin().trim().toUpperCase();
        String destination = request.destination().trim().toUpperCase();

        List<Map<String, Object>> aircraftList = jdbc.queryForList(
            "SELECT status FROM aeronaves WHERE callsign = ?", callsign
        );
        if (aircraftList.isEmpty()) {
            throw new IllegalArgumentException("Aeronave informada nao existe no sistema.");
        }
        if ("Em Voo".equals(aircraftList.get(0).get("status"))) {
            throw new IllegalStateException("Aeronave informada ja esta em voo ativo.");
        }

        jdbc.update(
            "UPDATE aeronaves SET status = 'Em Preparacao', ultima_atualizacao = NOW() WHERE callsign = ?",
            callsign
        );
        jdbc.update("UPDATE rotas_fms SET ativa = false WHERE callsign = ?", callsign);
        jdbc.update(
            "INSERT INTO rotas_fms (callsign, icao_origem, icao_destino, rota_texto, distancia_nm, eta_minutos, ativa) VALUES (?, ?, ?, 'Aguardando Calculo...', 0.0, 0, true)",
            callsign, origin, destination
        );

        kafkaProducer.sendRouteRequest(callsign, origin, destination);
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.queryForList("SELECT * FROM rotas_fms ORDER BY registrado_em DESC");
    }

    public void stopSimulation(String callsign) {
        callsign = callsign.trim().toUpperCase();
        jdbc.update(
            "UPDATE aeronaves SET status = 'No Patio', ultima_atualizacao = NOW() WHERE callsign = ?",
            callsign
        );
        jdbc.update("UPDATE rotas_fms SET ativa = false WHERE callsign = ?", callsign);
        kafkaProducer.sendSimulationEnded(callsign);
    }

    public void onRouteCalculated(String callsign, String origin, String destination,
                                   double distance, int eta, String routeText) {
        jdbc.update("UPDATE rotas_fms SET ativa = false WHERE callsign = ?", callsign);

        int updated = jdbc.update(
            "UPDATE rotas_fms SET rota_texto = ?, distancia_nm = ?, eta_minutos = ?, ativa = true WHERE callsign = ? AND icao_origem = ? AND icao_destino = ?",
            routeText, distance, eta, callsign, origin, destination
        );
        if (updated == 0) {
            jdbc.update(
                "INSERT INTO rotas_fms (callsign, icao_origem, icao_destino, rota_texto, distancia_nm, eta_minutos, ativa) VALUES (?, ?, ?, ?, ?, ?, true)",
                callsign, origin, destination, routeText, distance, eta
            );
        }

        jdbc.update(
            "UPDATE aeronaves SET status = 'Em Voo', ultima_atualizacao = NOW() WHERE callsign = ?",
            callsign
        );
    }
}
