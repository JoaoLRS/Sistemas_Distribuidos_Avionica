package avionica.audit.dto;

import java.util.List;
import java.util.Map;

public record TableDataDto(
    boolean success,
    String tableName,
    List<String> columns,
    List<Map<String, Object>> rows
) {
}
