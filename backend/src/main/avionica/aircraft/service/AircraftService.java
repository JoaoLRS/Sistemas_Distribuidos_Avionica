package avionica.aircraft.service;

import avionica.aircraft.dto.AircraftRequest;
import avionica.aircraft.model.Aircraft;
import avionica.aircraft.repository.AircraftRepository;
import avionica.kafka.producer.RouteKafkaProducer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final RouteKafkaProducer kafkaProducer;

    public AircraftService(AircraftRepository aircraftRepository, RouteKafkaProducer kafkaProducer) {
        this.aircraftRepository = aircraftRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public void create(AircraftRequest request) {
        String callsign = request.callsign().trim().toUpperCase();

        if (aircraftRepository.existsById(callsign)) {
            throw new IllegalArgumentException("Aeronave com este callsign ja existe.");
        }

        Aircraft aircraft = Aircraft.builder()
            .callsign(callsign)
            .modelo(request.modelo())
            .capacidadeCombustivel(request.capacidade_combustivel())
            .velocidadeCruzeiro(request.velocidade_cruzeiro())
            .status("No Patio")
            .ultimaAtualizacao(Instant.now())
            .build();

        aircraftRepository.save(aircraft);

        // Notificar no Kafka que a aeronave foi criada
        kafkaProducer.sendAircraftCreated(callsign, request.modelo());
    }

    public List<Aircraft> listAll() {
        return aircraftRepository.findAll();
    }

    public void delete(String callsign) {
        callsign = callsign.trim().toUpperCase();

        Optional<Aircraft> opt = aircraftRepository.findById(callsign);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Aeronave nao encontrada.");
        }
        Aircraft aircraft = opt.get();
        if ("Em Voo".equals(aircraft.getStatus())) {
            throw new IllegalStateException("Nao e possivel excluir uma aeronave que esta em voo.");
        }

        aircraftRepository.delete(aircraft);
    }
}
