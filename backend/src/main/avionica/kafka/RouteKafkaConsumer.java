package avionica.kafka;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RouteKafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RouteKafkaConsumer.class);

    private final JdbcTemplate jdbc;

    public RouteKafkaConsumer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

            // Desativar outras rotas para esta aeronave
            jdbc.update("UPDATE rotas_fms SET ativa = false WHERE callsign = ?", callsign);

            // Inserir ou Atualizar a rota
            int updated = jdbc.update(
                "UPDATE rotas_fms SET rota_texto = ?, distancia_nm = ?, eta_minutos = ?, ativa = true WHERE callsign = ? AND icao_origem = ? AND icao_destino = ?",
                routeText, distance, eta, callsign, origin, destination
            );

            if (updated == 0) {
                jdbc.update(
                    "INSERT INTO rotas_fms (callsign, icao_origem, icao_destino, rota_texto, distancia_nm, eta_minutos, ativa) VALUES (?, ?, ?, ?, ?, ?, true)",
                    callsign, origin, destination, routeText, distance, eta
                );
            }

            // Atualizar status da aeronave para 'Em Voo'
            jdbc.update("UPDATE aeronaves SET status = 'Em Voo', ultima_atualizacao = NOW() WHERE callsign = ?", callsign);
            logger.info("Aeronave {} atualizada para status 'Em Voo'", callsign);

        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de rota calculada do Kafka: {}", e.getMessage(), e);
        }
    }
}
