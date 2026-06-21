package avionica.route.dto;

public record RouteRequest(
    String callsign,
    String origin,
    String destination
) {}
