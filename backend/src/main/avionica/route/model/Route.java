package avionica.route.model;

public record Route(
    String callsign,
    String icaoOrigem,
    String icaoDestino,
    String rotaTexto,
    Double distanciaNm,
    Integer etaMinutos,
    Boolean ativa
) {}
