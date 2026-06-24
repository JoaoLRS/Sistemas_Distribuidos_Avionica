package avionica.aircraft.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "aeronaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aircraft {

    @Id
    @Column(name = "callsign", length = 20)
    private String callsign;

    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    @Column(name = "capacidade_combustivel")
    private Integer capacidadeCombustivel;

    @Column(name = "velocidade_cruzeiro")
    private Integer velocidadeCruzeiro;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "No Patio";

    @Column(name = "ultima_atualizacao", nullable = false)
    @Builder.Default
    private Instant ultimaAtualizacao = Instant.now();
}
