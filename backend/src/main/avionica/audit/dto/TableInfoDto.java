package avionica.audit.dto;

public record TableInfoDto(
    String id,
    String displayName,
    long rowCount
) {
}
