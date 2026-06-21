package avionica.aircraft.dto;

public record AircraftResponse(
    String callsign,
    String modelo,
    Integer capacidade_combustivel,
    Integer velocidade_cruzeiro,
    String status,
    String ultima_atualizacao
) {}
