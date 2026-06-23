package avionica.torrecomando.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import avionica.torrecomando.dto.AircraftRequest;

@Service
public class AircraftService {

    private static final Logger log = LoggerFactory.getLogger(AircraftService.class);

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public AircraftService(JdbcTemplate jdbc, KafkaTemplate<String, String> kafkaTemplate) {
        this.jdbc = jdbc;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Cadastra uma nova aeronave com status inicial "No Patio"
     * e publica evento no Kafka (avionica.aircraft.created).
     */
    public void create(AircraftRequest request) {
        if (request.callsign() == null || request.callsign().isBlank()) {
            throw new IllegalArgumentException("Callsign é obrigatório.");
        }
        String callsign = request.callsign().trim().toUpperCase();

        if (callsign.length() < 4 || callsign.length() > 8) {
            throw new IllegalArgumentException("Callsign deve ter entre 4 e 8 caracteres.");
        }

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM aeronaves WHERE callsign = ?",
                Integer.class, callsign);

        if (count != null && count > 0) {
            throw new IllegalArgumentException("Callsign já cadastrado: " + callsign);
        }

        jdbc.update("""
                INSERT INTO aeronaves (callsign, modelo, capacidade_combustivel, velocidade_cruzeiro, status, ultima_atualizacao)
                VALUES (?, ?, ?, ?, 'No Patio', NOW())
                """,
                callsign,
                request.modelo(),
                request.capacidade_combustivel(),
                request.velocidade_cruzeiro()
        );


        try {
            JSONObject event = new JSONObject();
            event.put("callsign", callsign);
            event.put("modelo", request.modelo());
            event.put("capacidade_combustivel", request.capacidade_combustivel());
            event.put("velocidade_cruzeiro", request.velocidade_cruzeiro());
            event.put("status", "No Patio");
            event.put("timestamp", Instant.now().toString());
            event.put("source", "aerocontrol-dispatcher");

            kafkaTemplate.send("avionica.aircraft.created", callsign, event.toString());
            log.info("Aeronave {} criada e evento publicado no Kafka", callsign);
        } catch (Exception e) {
            log.warn("Aeronave {} criada no banco, mas falha ao publicar no Kafka: {}", callsign, e.getMessage());
        }
    }

    /** Lista todas as aeronaves cadastradas, ordenadas por callsign. */
    public List<Map<String, Object>> listAll() {
        return jdbc.queryForList("SELECT * FROM aeronaves ORDER BY callsign");
    }

    /**
     * Exclui uma aeronave pelo callsign.
     * Bloqueia exclusão se a aeronave estiver "Em Voo".
     */
    public void delete(String callsign) {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT status FROM aeronaves WHERE callsign = ?", callsign);

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Aeronave não encontrada: " + callsign);
        }

        String status = (String) results.getFirst().get("status");

        if ("Em Voo".equals(status)) {
            throw new IllegalStateException("Não é possível excluir uma aeronave em voo: " + callsign);
        }

        jdbc.update("DELETE FROM aeronaves WHERE callsign = ?", callsign);
        log.info("Aeronave {} removida do sistema", callsign);
    }
}