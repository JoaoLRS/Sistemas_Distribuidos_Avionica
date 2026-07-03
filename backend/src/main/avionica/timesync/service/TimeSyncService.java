package avionica.timesync.service;

import avionica.timesync.dto.TimeSyncResponse;
import org.springframework.stereotype.Service;

@Service
public class TimeSyncService {

    
    public TimeSyncResponse getCurrentTime() {
        long nano = System.nanoTime();
        return new TimeSyncResponse(
            nano,
            nano,
            System.currentTimeMillis()
        );
    }
}
