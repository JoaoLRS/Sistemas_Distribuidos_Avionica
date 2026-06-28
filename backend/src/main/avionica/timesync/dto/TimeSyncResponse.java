package avionica.timesync.dto;

import lombok.Builder;

@Builder
public record TimeSyncResponse(
    long server_time,
    long server_time_ns,
    long server_time_ms
) {}
