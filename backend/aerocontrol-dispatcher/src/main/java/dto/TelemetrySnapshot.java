package avionica.torrecomando.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelemetrySnapshot {
    private Map<String, Object> voo;
    private Map<String, Object> radar;
    private Map<String, Object> navegacao;
    private Map<String, Object> freios;
    private Map<String, Object> waic;
    private Map<String, Object> rotaAtiva;
    private List<Map<String, Object>> alertas;
}