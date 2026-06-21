package avionica.api;

import avionica.timesync.model.TimeSyncResponse;
import avionica.timesync.service.TimeSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TimeSyncController {

    private final TimeSyncService timeSyncService;

    public TimeSyncController(TimeSyncService timeSyncService) {
        this.timeSyncService = timeSyncService;
    }

    @GetMapping("/time-sync")
    public TimeSyncResponse getServerTime() {
        return timeSyncService.getCurrentTime();
    }
}
