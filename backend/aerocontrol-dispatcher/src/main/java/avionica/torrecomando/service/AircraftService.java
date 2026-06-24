package avionica.torrecomando.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import avionica.torrecomando.model.Aircraft;
import avionica.torrecomando.repository.AircraftRepository;

@Service
public class AircraftService {

    private static final Logger log = LoggerFactory.getLogger(AircraftService.class);

    private final AircraftRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public AircraftService(AircraftRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Cadastra uma nova aeronave com status inicial "No Patio"
     * e publica evento no Kafka (avionica.aircraft.created).
     */
    public void create(avionica.torrecomando.dto.AircraftRequestDto request) {
        if (request.getCallsign() == null || request.getCallsign().isBlank()) {
            throw new IllegalArgumentException("Callsign é obrigatório.");
        }
        String callsign = request.getCallsign().trim().toUpperCase();

        if (callsign.length() < 4 || callsign.length() > 8) {
            throw new IllegalArgumentException("Callsign deve ter entre 4 e 8 caracteres.");
        }

        if (repository.existsByCallsign(callsign)) {
            throw new IllegalArgumentException("Callsign já cadastrado: " + callsign);
        }

        repository.insert(callsign, request.getModelo(),
                request.getCapacidade_combustivel(), request.getVelocidade_cruzeiro());

        try {
            JSONObject event = new JSONObject();
            event.put("callsign", callsign);
            event.put("modelo", request.getModelo());
            event.put("capacidade_combustivel", request.getCapacidade_combustivel());
            event.put("velocidade_cruzeiro", request.getVelocidade_cruzeiro());
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
    public List<Aircraft> listAll() {
        return repository.findAll();
    }

    /** Lista aeronaves filtradas por status. */
    public List<Aircraft> listByStatus(String status) {
        return repository.findByStatus(status);
    }

    /** Busca uma aeronave pelo callsign. */
    public Aircraft findByCallsign(String callsign) {
        return repository.findByCallsign(callsign)
                .orElseThrow(() -> new IllegalArgumentException("Aeronave não encontrada: " + callsign));
    }

    /** Atualiza o status de uma aeronave. */
    public void updateStatus(String callsign, String novoStatus) {
        Aircraft aircraft = findByCallsign(callsign);

        // Validação: não pode mudar status de aeronave em voo para "No Patio" diretamente
        if ("Em Voo".equals(aircraft.getStatus()) && "No Patio".equals(novoStatus)) {
            throw new IllegalStateException(
                    "Aeronave em voo não pode voltar para 'No Patio' diretamente. Use 'Em Preparacao' primeiro.");
        }

        repository.updateStatus(callsign, novoStatus);
        log.info("Status da aeronave {} atualizado para '{}'", callsign, novoStatus);
    }

    /**
     * Exclui uma aeronave pelo callsign.
     * Bloqueia exclusão se a aeronave estiver "Em Voo".
     */
    public void delete(String callsign) {
        Aircraft aircraft = findByCallsign(callsign);

        if ("Em Voo".equals(aircraft.getStatus())) {
            throw new IllegalStateException("Não é possível excluir uma aeronave em voo: " + callsign);
        }

        repository.deleteByCallsign(callsign);
        log.info("Aeronave {} removida do sistema", callsign);
    }

    /** Snapshot consolidado de telemetria para a Torre de Comando. */
    public Map<String, Object> getTelemetrySnapshot() {
        return repository.findTelemetrySnapshot();
    }

    /** Estatísticas do painel: total e por status. */
    public Map<String, Object> getStats() {
        return Map.of(
                "total", repository.countAll(),
                "noPatio", repository.countByStatus("No Patio"),
                "emPreparacao", repository.countByStatus("Em Preparacao"),
                "emVoo", repository.countByStatus("Em Voo")
        );
    }
}