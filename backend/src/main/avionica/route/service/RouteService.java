package avionica.route.service;

import avionica.aircraft.model.Aircraft;
import avionica.aircraft.repository.AircraftRepository;
import avionica.kafka.producer.RouteKafkaProducer;
import avionica.route.dto.RouteRequest;
import avionica.route.model.Route;
import avionica.route.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final AircraftRepository aircraftRepository;
    private final RouteKafkaProducer kafkaProducer;

    public RouteService(RouteRepository routeRepository, AircraftRepository aircraftRepository, RouteKafkaProducer kafkaProducer) {
        this.routeRepository = routeRepository;
        this.aircraftRepository = aircraftRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public void requestRoute(RouteRequest request) {
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

        kafkaProducer.sendRouteRequest(callsign, origin, destination);
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

        Optional<Route> optRoute = routeRepository.findFirstByCallsignAndIcaoOrigemAndIcaoDestinoOrderByRegistradoEmDesc(callsign, origin, destination);
        if (optRoute.isPresent()) {
            Route route = optRoute.get();
            route.setRotaTexto(routeText);
            route.setDistanciaNm(distance);
            route.setEtaMinutos(eta);
            route.setAtiva(true);
            routeRepository.save(route);
        } else {
            Route route = Route.builder()
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
    }
}
