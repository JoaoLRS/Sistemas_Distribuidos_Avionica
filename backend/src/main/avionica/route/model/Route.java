package avionica.route.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rotas_fms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "registrado_em", nullable = false)
    @Builder.Default
    private Instant registradoEm = Instant.now();

    @Column(name = "callsign", length = 20)
    private String callsign;

    @Column(name = "icao_origem", length = 4)
    private String icaoOrigem;

    @Column(name = "icao_destino", length = 4)
    private String icaoDestino;

    @Column(name = "rota_texto", length = 200)
    @Builder.Default
    private String rotaTexto = "Aguardando Calculo...";

    @Column(name = "distancia_nm")
    @Builder.Default
    private Double distanciaNm = 0.0;

    @Column(name = "eta_minutos")
    @Builder.Default
    private Integer etaMinutos = 0;

    @Column(name = "ativa")
    @Builder.Default
    private Boolean ativa = true;
}
