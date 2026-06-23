package avionica.api.controler;

import avionica.aircraft.dto.AircraftRequest;
import avionica.aircraft.service.AircraftService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aircraft")
public class AircraftController {

    private final AircraftService aircraftService;

    public AircraftController(AircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AircraftRequest body) {
        try {
            aircraftService.create(body);
            return ResponseEntity.ok(Map.of(
                "mensagem", "Aeronave cadastrada.",
                "callsign", body.callsign().toUpperCase()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao cadastrar aeronave: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        try {
            return ResponseEntity.ok(aircraftService.listAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{callsign}")
    public ResponseEntity<?> delete(@PathVariable String callsign) {
        if (callsign == null || callsign.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            aircraftService.delete(callsign);
            return ResponseEntity.ok(Map.of("mensagem", "Aeronave excluida com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao excluir aeronave: " + e.getMessage()));
        }
    }
}
