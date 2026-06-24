package avionica.telemetry.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AircraftDataSnapshot {
    private Instant updatedAt;
    private Map<String, Object> flight;
    private Map<String, Object> brakes;
    private Map<String, Object> radar;
    private Map<String, Object> fms;
    private Map<String, Object> navigation;
    private Map<String, Object> waic;
    private Map<String, Object> antiIce;
    private Map<String, Object> alerts;
    private List<AircraftMessage> rawMessages;
}
