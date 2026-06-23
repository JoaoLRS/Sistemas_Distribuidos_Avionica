package avionica.torrecomando.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import avionica.torrecomando.dto.AircraftRequest;
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
    public ResponseEntity<Map<String, String>> create(@RequestBody AircraftRequest request) {
        try {
            service.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Aeronave criada com sucesso",
                            "callsign", request.callsign().trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** CDU-02: Lista todas as aeronaves cadastradas. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listAll() {
        return ResponseEntity.ok(service.listAll());
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
}