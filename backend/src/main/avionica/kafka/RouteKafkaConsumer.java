package avionica.kafka;

import avionica.route.service.RouteService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RouteKafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RouteKafkaConsumer.class);

    private final RouteService routeService;

    public RouteKafkaConsumer(RouteService routeService) {
        this.routeService = routeService;
    }

    @KafkaListener(topics = "avionica.route.calculated", groupId = "backend-gateway")
    public void onRouteCalculated(String message) {
        logger.info("Evento de rota calculada recebido do Kafka: {}", message);
        try {
            JSONObject json = new JSONObject(message);
            String callsign = json.getString("callsign").toUpperCase();
            String origin = json.getString("origem").toUpperCase();
            String destination = json.getString("destino").toUpperCase();
            double distance = json.getDouble("distancia_nm");
            int eta = json.getInt("eta_minutos");
            String routeText = json.optString("rota_texto", origin + " -> " + destination);

            routeService.onRouteCalculated(callsign, origin, destination, distance, eta, routeText);
            logger.info("Aeronave {} atualizada para status 'Em Voo' pelo servico de rotas", callsign);

        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de rota calculada do Kafka: {}", e.getMessage(), e);
        }
    }
}
