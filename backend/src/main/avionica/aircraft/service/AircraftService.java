package avionica.aircraft.service;

import avionica.aircraft.dto.AircraftRequest;
import avionica.kafka.RouteKafkaProducer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AircraftService {

    private final JdbcTemplate jdbc;
    private final RouteKafkaProducer kafkaProducer;

    public AircraftService(JdbcTemplate jdbc, RouteKafkaProducer kafkaProducer) {
        this.jdbc = jdbc;
        this.kafkaProducer = kafkaProducer;
    }

    public void create(AircraftRequest request) {
        String callsign = request.callsign().trim().toUpperCase();

        List<Map<String, Object>> existing = jdbc.queryForList(
            "SELECT callsign FROM aeronaves WHERE callsign = ?", callsign
        );
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Aeronave com este callsign ja existe.");
        }

        jdbc.update(
            "INSERT INTO aeronaves (callsign, modelo, capacidade_combustivel, velocidade_cruzeiro, status, ultima_atualizacao) VALUES (?, ?, ?, ?, 'No Patio', NOW())",
            callsign, request.modelo(), request.capacidade_combustivel(), request.velocidade_cruzeiro()
        );

        // Notificar no Kafka que a aeronave foi criada
        kafkaProducer.sendAircraftCreated(callsign, request.modelo());
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.queryForList("SELECT * FROM aeronaves ORDER BY callsign");
    }

    public void delete(String callsign) {
        callsign = callsign.trim().toUpperCase();

        List<Map<String, Object>> aircraft = jdbc.queryForList(
            "SELECT status FROM aeronaves WHERE callsign = ?", callsign
        );
        if (aircraft.isEmpty()) {
            throw new IllegalArgumentException("Aeronave nao encontrada.");
        }
        if ("Em Voo".equals(aircraft.get(0).get("status"))) {
            throw new IllegalStateException("Nao e possivel excluir uma aeronave que esta em voo.");
        }

        jdbc.update("DELETE FROM aeronaves WHERE callsign = ?", callsign);
    }
}
