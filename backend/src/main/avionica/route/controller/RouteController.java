package avionica.route.controller;

import avionica.route.dto.RouteRequest;
import avionica.route.dto.RouteResponse;
import avionica.route.model.Route;
import avionica.route.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    public ResponseEntity<?> requestRoute(@RequestBody RouteRequest body) {
        if (body.callsign() == null || body.origin() == null || body.destination() == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Callsign, origin e destination sao obrigatorios."));
        }
        try {
            routeService.requestRoute(body);
            return ResponseEntity.ok(new RouteResponse(
                "PENDING",
                body.callsign().toUpperCase(),
                body.origin().toUpperCase(),
                body.destination().toUpperCase(),
                "Solicitacao enviada com sucesso ao FMS."
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao processar rota: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Route>> list() {
        try {
            return ResponseEntity.ok(routeService.listAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopSimulation(@RequestBody Map<String, String> body) {
        String callsign = body.get("callsign");
        if (callsign == null || callsign.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Callsign e obrigatorio para encerrar."));
        }
        try {
            routeService.stopSimulation(callsign);
            return ResponseEntity.ok(Map.of("mensagem", "Simulacao encerrada com sucesso.", "callsign", callsign.toUpperCase()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro ao encerrar simulacao: " + e.getMessage()));
        }
    }
}
