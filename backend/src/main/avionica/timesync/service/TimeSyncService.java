package avionica.timesync.service;

import avionica.timesync.model.TimeSyncResponse;
import org.springframework.stereotype.Service;

@Service
public class TimeSyncService {

    /**
     * Retorna o tempo atual do servidor para que os sensores distribuídos
     * possam calibrar seus relógios locais usando o Algoritmo de Cristian.
     *
     * O sensor deve:
     *   1. Registrar T0 = time.time_ns() antes de chamar este endpoint.
     *   2. Chamar GET /api/time-sync e receber server_time_ns.
     *   3. Registrar T1 = time.time_ns() após o retorno.
     *   4. Calcular RTT = T1 - T0.
     *   5. Calcular TempoSync = server_time_ns + (RTT / 2).
     */
    public TimeSyncResponse getCurrentTime() {
        return new TimeSyncResponse(
            System.nanoTime(),
            System.currentTimeMillis()
        );
    }
}
