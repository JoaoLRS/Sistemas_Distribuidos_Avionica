package avionica.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RouteKafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(RouteKafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public RouteKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRouteRequest(String callsign, String origin, String destination) {
        try {
            String payload = String.format(
                "{\"callsign\":\"%s\",\"origem\":\"%s\",\"destino\":\"%s\"}",
                callsign, origin, destination
            );
            kafkaTemplate.send("avionica.route.requested", callsign, payload);
            logger.info("Solicitacao de rota enviada ao Kafka: {}", payload);
        } catch (Exception e) {
            logger.warn("Falha ao enviar solicitacao de rota ao Kafka (continuando operacao local): {}", e.getMessage());
        }
    }

    public void sendSimulationEnded(String callsign) {
        try {
            String payload = String.format("{\"callsign\":\"%s\",\"status\":\"ended\"}", callsign);
            kafkaTemplate.send("avionica.simulation.ended", callsign, payload);
            logger.info("Evento de simulacao encerrada enviado ao Kafka para {}", callsign);
        } catch (Exception e) {
            logger.warn("Falha ao enviar simulacao encerrada ao Kafka: {}", e.getMessage());
        }
    }

    public void sendAircraftCreated(String callsign, String modelo) {
        try {
            String payload = String.format("{\"callsign\":\"%s\",\"modelo\":\"%s\"}", callsign, modelo);
            kafkaTemplate.send("avionica.aircraft.created", callsign, payload);
            logger.info("Evento de criacao de aeronave enviado ao Kafka: {}", payload);
        } catch (Exception e) {
            logger.warn("Falha ao enviar evento de criacao de aeronave ao Kafka: {}", e.getMessage());
        }
    }
}
