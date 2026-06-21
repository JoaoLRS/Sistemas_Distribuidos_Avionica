package avionica.telemetry.model;

import java.time.Instant;
import java.util.Map;

public record AircraftMessage(
    String topic,
    Instant receivedAt,
    Map<String, Object> payload
) {
}
