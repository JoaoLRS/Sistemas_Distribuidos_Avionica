package avionica.audit.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record CausalityReportDto(
    boolean success,
    OffsetDateTime timestamp,
    Map<String, Object> metricasGlobais,
    List<SensorAuditDto> sensores
) {
}
