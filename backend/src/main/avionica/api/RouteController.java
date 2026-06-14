package avionica.api;

import avionica.kafka.RouteKafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final JdbcTemplate jdbc;
    private final RouteKafkaProducer kafkaProducer;

    public RouteController(JdbcTemplate jdbc, RouteKafkaProducer kafkaProducer) {
        this.jdbc = jdbc;
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping
    public ResponseEntity<?> requestRoute(@RequestBody Map<String, Object> body) {
        String callsign = (String) body.get("callsign");
        String origin = (String) body.get("origin");
        String destination = (String) body.get("destination");

        if (callsign == null || origin == null || destination == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Callsign, origin e destination sao obrigatorios."));
        }

        callsign = callsign.trim().toUpperCase();
        origin = origin.trim().toUpperCase();
        destination = destination.trim().toUpperCase();

        try {
            // Verificar se a aeronave existe
            List<Map<String, Object>> aircraftList = jdbc.queryForList("SELECT status FROM aeronaves WHERE callsign = ?", callsign);
            if (aircraftList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Aeronave informada nao existe no sistema."));
            }

            String status = (String) aircraftList.get(0).get("status");
            if ("Em Voo".equals(status)) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Aeronave informada ja esta em voo ativo."));
            }

            // Atualizar status para 'Em Preparacao'
            jdbc.update("UPDATE aeronaves SET status = 'Em Preparacao', ultima_atualizacao = NOW() WHERE callsign = ?", callsign);

            // Desativar rotas anteriores para essa aeronave
            jdbc.update("UPDATE rotas_fms SET ativa = false WHERE callsign = ?", callsign);

            // Inserir registro de rota PENDENTE
            jdbc.update(
                "INSERT INTO rotas_fms (callsign, icao_origem, icao_destino, rota_texto, distancia_nm, eta_minutos, ativa) VALUES (?, ?, ?, 'Aguardando Calculo...', 0.0, 0, true)",
                callsign, origin, destination
            );

            // Enviar solicitacao via Kafka
            kafkaProducer.sendRouteRequest(callsign, origin, destination);

            return ResponseEntity.ok(Map.of(
                "status", "PENDING",
                "callsign", callsign,
                "origem", origin,
                "destino", destination,
                "mensagem", "Solicitacao enviada com sucesso ao FMS."
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao processar rota: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        try {
            List<Map<String, Object>> list = jdbc.queryForList("SELECT * FROM rotas_fms ORDER BY registrado_em DESC");
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopSimulation(@RequestBody Map<String, String> body) {
        String callsign = body.get("callsign");
        if (callsign == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Callsign e obrigatorio para encerrar."));
        }

        callsign = callsign.trim().toUpperCase();

        try {
            // Atualizar status para 'No Patio'
            jdbc.update("UPDATE aeronaves SET status = 'No Patio', ultima_atualizacao = NOW() WHERE callsign = ?", callsign);

            // Desativar rota ativa
            jdbc.update("UPDATE rotas_fms SET ativa = false WHERE callsign = ?", callsign);

            // Enviar sinal de termino ao Kafka para parar os sensores
            kafkaProducer.sendSimulationEnded(callsign);

            return ResponseEntity.ok(Map.of("mensagem", "Simulacao encerrada com sucesso.", "callsign", callsign));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao encerrar simulacao: " + e.getMessage()));
        }
    }
}
