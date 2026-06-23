package avionica.torrecomando.model;
import java.time.Instant;
public record Aircraft(
        String callsign,
        String modelo,
        Integer capacidadeCombustivel,
        Integer velocidadeCruzeiro,
        String status,
        Instant ultimaAtualizacao
) {}