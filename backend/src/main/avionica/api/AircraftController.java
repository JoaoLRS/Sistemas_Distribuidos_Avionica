package avionica.api;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aircraft")
public class AircraftController {

    private final JdbcTemplate jdbc;

    public AircraftController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String callsign = (String) body.get("callsign");
        String modelo = (String) body.get("modelo");
        Integer capacidadeCombustivel = (Integer) body.get("capacidade_combustivel");
        Integer velocidadeCruzeiro = (Integer) body.get("velocidade_cruzeiro");

        if (callsign == null || callsign.trim().isEmpty() || modelo == null || modelo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Callsign e modelo sao obrigatorios."));
        }

        callsign = callsign.trim().toUpperCase();

        try {
            // Verificar se ja existe
            List<Map<String, Object>> existing = jdbc.queryForList("SELECT callsign FROM aeronaves WHERE callsign = ?", callsign);
            if (!existing.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Aeronave com este callsign ja existe."));
            }

            jdbc.update(
                "INSERT INTO aeronaves (callsign, modelo, capacidade_combustivel, velocidade_cruzeiro, status, ultima_atualizacao) VALUES (?, ?, ?, ?, 'No Patio', NOW())",
                callsign, modelo, capacidadeCombustivel, velocidadeCruzeiro
            );

            // TODO: Aqui a equipe podera publicar o evento avionica.aircraft.created no Kafka no futuro se desejado
            
            return ResponseEntity.ok(Map.of(
                "mensagem", "Aeronave cadastrada com sucesso.",
                "callsign", callsign,
                "status", "No Patio"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao cadastrar aeronave: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        try {
            List<Map<String, Object>> list = jdbc.queryForList("SELECT * FROM aeronaves ORDER BY callsign");
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{callsign}")
    public ResponseEntity<?> delete(@PathVariable String callsign) {
        if (callsign == null) {
            return ResponseEntity.badRequest().build();
        }

        callsign = callsign.trim().toUpperCase();

        try {
            List<Map<String, Object>> aircraft = jdbc.queryForList("SELECT status FROM aeronaves WHERE callsign = ?", callsign);
            if (aircraft.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String status = (String) aircraft.get(0).get("status");
            if ("Em Voo".equals(status)) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Nao e possivel excluir uma aeronave que esta em voo."));
            }

            jdbc.update("DELETE FROM aeronaves WHERE callsign = ?", callsign);
            return ResponseEntity.ok(Map.of("mensagem", "Aeronave excluida com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao excluir aeronave: " + e.getMessage()));
        }
    }
}
