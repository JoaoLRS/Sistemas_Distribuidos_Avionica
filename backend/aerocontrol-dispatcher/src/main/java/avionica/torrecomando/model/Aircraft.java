package avionica.torrecomando.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Aircraft {
    private String callsign;
    private String modelo;
    private Integer capacidadeCombustivel;
    private Integer velocidadeCruzeiro;
    private String status;
    private Instant ultimaAtualizacao;
}