package avionica.route.dto;

public record RouteResponse(
    String status,
    String callsign,
    String origem,
    String destino,
    String mensagem
) {}
