package avionica.route.service;

import avionica.aircraft.model.Aircraft;
import avionica.aircraft.repository.AircraftRepository;
import avionica.config.CustomCircuitBreaker;
import avionica.kafka.producer.RouteKafkaProducer;
import avionica.route.dto.RouteRequest;
import avionica.route.model.Route;
import avionica.route.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class RouteService {
    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final AircraftRepository aircraftRepository;
    private final RouteKafkaProducer kafkaProducer;

    private final CustomCircuitBreaker circuitBreaker = new CustomCircuitBreaker("FMS-CircuitBreaker", 3, 10000);
    private final Map<String, CompletableFuture<Route>> pendingRequests = new ConcurrentHashMap<>();

    public RouteService(RouteRepository routeRepository, AircraftRepository aircraftRepository, RouteKafkaProducer kafkaProducer) {
        this.routeRepository = routeRepository;
        this.aircraftRepository = aircraftRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public Route requestRoute(RouteRequest request) {
        String callsign   = request.callsign().trim().toUpperCase();
        String origin     = request.origin().trim().toUpperCase();
        String destination = request.destination().trim().toUpperCase();

        Optional<Aircraft> optAircraft = aircraftRepository.findById(callsign);
        if (optAircraft.isEmpty()) {
            throw new IllegalArgumentException("Aeronave informada nao existe no sistema.");
        }

        Aircraft aircraft = optAircraft.get();
        if ("Em Voo".equals(aircraft.getStatus())) {
            throw new IllegalStateException("Aeronave informada ja esta em voo ativo.");
        }

        if (!circuitBreaker.canExecute()) {
            logger.warn("Circuit Breaker OPEN para FMS. Disparando fallback imediato para {}", callsign);
            return executeFallback(aircraft, origin, destination);
        }

        aircraft.setStatus("Em Preparacao");
        aircraft.setUltimaAtualizacao(Instant.now());
        aircraftRepository.save(aircraft);

        routeRepository.deactivatePreviousRoutes(callsign);

        Route route = Route.builder()
            .callsign(callsign)
            .icaoOrigem(origin)
            .icaoDestino(destination)
            .rotaTexto("Aguardando Calculo...")
            .distanciaNm(0.0)
            .etaMinutos(0)
            .ativa(true)
            .registradoEm(Instant.now())
            .build();
        routeRepository.save(route);

        CompletableFuture<Route> future = new CompletableFuture<>();
        pendingRequests.put(callsign, future);

        kafkaProducer.sendRouteRequest(callsign, origin, destination);

        try {
            Route calculatedRoute = future.get(3, TimeUnit.SECONDS);
            circuitBreaker.recordSuccess();
            return calculatedRoute;
        } catch (TimeoutException e) {
            circuitBreaker.recordFailure();
            logger.warn("Timeout de 3s no FMS para {}. Executando fallback...", callsign);
            return executeFallback(aircraft, origin, destination);
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            logger.warn("Erro ao calcular rota via FMS para {}: {}. Executando fallback...", callsign, e.getMessage());
            return executeFallback(aircraft, origin, destination);
        } finally {
            pendingRequests.remove(callsign);
        }
    }

    private Route executeFallback(Aircraft aircraft, String origin, String destination) {
        String callsign = aircraft.getCallsign();
        routeRepository.deactivatePreviousRoutes(callsign);

        double distance = 240.0;
        int eta = 40; 
        String routeText = String.format("%s ➔ [FALLBACK] ➔ %s", origin, destination);

        Route fallbackRoute = Route.builder()
            .callsign(callsign)
            .icaoOrigem(origin)
            .icaoDestino(destination)
            .rotaTexto(routeText)
            .distanciaNm(distance)
            .etaMinutos(eta)
            .ativa(true)
            .registradoEm(Instant.now())
            .build();
        routeRepository.save(fallbackRoute);

        aircraft.setStatus("Em Voo");
        aircraft.setUltimaAtualizacao(Instant.now());
        aircraftRepository.save(aircraft);

        return fallbackRoute;
    }

    public List<Route> listAll() {
        return routeRepository.findAllByOrderByRegistradoEmDesc();
    }

    public void stopSimulation(String callsign) {
        callsign = callsign.trim().toUpperCase();

        Optional<Aircraft> optAircraft = aircraftRepository.findById(callsign);
        if (optAircraft.isPresent()) {
            Aircraft aircraft = optAircraft.get();
            aircraft.setStatus("No Patio");
            aircraft.setUltimaAtualizacao(Instant.now());
            aircraftRepository.save(aircraft);
        }

        routeRepository.deactivatePreviousRoutes(callsign);
        kafkaProducer.sendSimulationEnded(callsign);
    }

    public void onRouteCalculated(String callsign, String origin, String destination,
                                   double distance, int eta, String routeText) {
        routeRepository.deactivatePreviousRoutes(callsign);

        Route route;
        Optional<Route> optRoute = routeRepository.findFirstByCallsignAndIcaoOrigemAndIcaoDestinoOrderByRegistradoEmDesc(callsign, origin, destination);
        if (optRoute.isPresent()) {
            route = optRoute.get();
            route.setRotaTexto(routeText);
            route.setDistanciaNm(distance);
            route.setEtaMinutos(eta);
            route.setAtiva(true);
            routeRepository.save(route);
        } else {
            route = Route.builder()
                .callsign(callsign)
                .icaoOrigem(origin)
                .icaoDestino(destination)
                .rotaTexto(routeText)
                .distanciaNm(distance)
                .etaMinutos(eta)
                .ativa(true)
                .registradoEm(Instant.now())
                .build();
            routeRepository.save(route);
        }

        Optional<Aircraft> optAircraft = aircraftRepository.findById(callsign);
        if (optAircraft.isPresent()) {
            Aircraft aircraft = optAircraft.get();
            aircraft.setStatus("Em Voo");
            aircraft.setUltimaAtualizacao(Instant.now());
            aircraftRepository.save(aircraft);
        }

        CompletableFuture<Route> future = pendingRequests.get(callsign);
        if (future != null) {
            future.complete(route);
        }
    }
}
