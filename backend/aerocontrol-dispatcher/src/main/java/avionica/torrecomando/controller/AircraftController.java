package avionica.torrecomando.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import avionica.torrecomando.model.Aircraft;
import avionica.torrecomando.service.AircraftService;

@RestController
@RequestMapping("/api/aircraft")
public class AircraftController {

    private final AircraftService service;

    public AircraftController(AircraftService service) {
        this.service = service;
    }

    /** CDU-01: Cadastra uma nova aeronave. */
    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody avionica.torrecomando.dto.AircraftRequestDto request) {
        try {
            service.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Aeronave criada com sucesso",
                            "callsign", request.getCallsign().trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** CDU-02: Lista todas as aeronaves cadastradas (com filtro opcional por status). */
    @GetMapping
    public ResponseEntity<List<Aircraft>> listAll(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(service.listByStatus(status));
        }
        return ResponseEntity.ok(service.listAll());
    }

    /** CDU-02: Busca aeronave por callsign. */
    @GetMapping("/{callsign}")
    public ResponseEntity<?> findByCallsign(@PathVariable String callsign) {
        try {
            return ResponseEntity.ok(service.findByCallsign(callsign));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** CDU-03: Atualiza o status de uma aeronave. */
    @PatchMapping("/{callsign}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String callsign,
            @RequestBody Map<String, String> body) {
        try {
            String novoStatus = body.get("status");
            if (novoStatus == null || novoStatus.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Campo 'status' é obrigatório."));
            }
            service.updateStatus(callsign, novoStatus);
            return ResponseEntity.ok(Map.of(
                    "message", "Status atualizado com sucesso",
                    "callsign", callsign,
                    "status", novoStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** CDU-02: Exclui aeronave (bloqueia se Em Voo). */
    @DeleteMapping("/{callsign}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String callsign) {
        try {
            service.delete(callsign);
            return ResponseEntity.ok(Map.of("message", "Aeronave removida com sucesso"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Estatísticas: total de aeronaves e contagem por status. */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    /** Snapshot de telemetria consolidado para a Torre de Comando. */
    @GetMapping("/telemetry/snapshot")
    public ResponseEntity<Map<String, Object>> getTelemetrySnapshot() {
        return ResponseEntity.ok(service.getTelemetrySnapshot());
    }
}