package avionica.timesync.model;

public record TimeSyncResponse(
    long server_time_ns,
    long server_time_ms
) {}
