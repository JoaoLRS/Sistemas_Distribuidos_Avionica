package avionica.kafka.consumer;

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
            JSONObject root = new JSONObject(message);
            
            // Se a mensagem vem embrulhada pela bridge (contendo "payload")
            JSONObject payload = root.has("payload") ? root.getJSONObject("payload") : root;
            
            // O payload do FMS contem a chave "dados"
            JSONObject dados = payload.has("dados") ? payload.getJSONObject("dados") : payload;

            // Se for a mensagem inicial "AGUARDANDO" sem rota real, ignoramos
            if ("AGUARDANDO".equalsIgnoreCase(dados.optString("status")) && !dados.has("callsign") && !payload.has("callsign")) {
                logger.info("[RouteKafkaConsumer] Rota inicial com status AGUARDANDO. Ignorando processamento.");
                return;
            }

            // Lê as informações com chaves alternativas e fallbacks
            String callsign = payload.has("callsign") ? payload.getString("callsign") : dados.optString("callsign", "UNKNOWN");
            String origin = dados.has("origem") ? dados.getString("origem") : dados.optString("origin", "N/A");
            String destination = dados.has("destino") ? dados.getString("destino") : dados.optString("destination", "N/A");
            double distance = dados.optDouble("distancia_nm", 0.0);
            int eta = dados.optInt("eta_minutos", 0);
            String routeText = dados.optString("rota_texto", origin + " -> " + destination);

            if ("UNKNOWN".equalsIgnoreCase(callsign) || "N/A".equalsIgnoreCase(origin)) {
                logger.debug("[RouteKafkaConsumer] Ignorando mensagem de rota sem dados cruciais (callsign ou origem nulos).");
                return;
            }

            routeService.onRouteCalculated(callsign.toUpperCase(), origin.toUpperCase(), destination.toUpperCase(), 
                                           distance, eta, routeText);
            logger.info("Aeronave {} atualizada para status 'Em Voo' pelo servico de rotas", callsign);

        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de rota calculada do Kafka: {}", e.getMessage(), e);
        }
    }
}
