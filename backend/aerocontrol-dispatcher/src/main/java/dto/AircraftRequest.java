package avionica.torrecomando.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AircraftRequest {
    private String callsign;
    private String modelo;
    private Integer capacidade_combustivel;
    private Integer velocidade_cruzeiro;
}