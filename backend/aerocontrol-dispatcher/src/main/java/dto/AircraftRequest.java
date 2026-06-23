package avionica.torrecomando.dto;

public record AircraftRequest(
        String callsign,
        String modelo,
        Integer capacidade_combustivel,
        Integer velocidade_cruzeiro
) {}