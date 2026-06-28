package avionica.config;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomCircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(CustomCircuitBreaker.class);

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final int failureThreshold; 
    private final long retryPeriodMs;   
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private long lastStateTransitionTime = System.currentTimeMillis();

    public CustomCircuitBreaker(String name, int failureThreshold, long retryPeriodMs) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.retryPeriodMs = retryPeriodMs;
    }

    public boolean canExecute() {
        State currentState = state.get();
        if (currentState == State.OPEN) {
            if (System.currentTimeMillis() - lastStateTransitionTime > retryPeriodMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    lastStateTransitionTime = System.currentTimeMillis();
                    logger.info("Circuit Breaker [{}] transicionado de OPEN para HALF_OPEN. Tentando chamada de teste.", name);
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        State currentState = state.get();
        if (currentState == State.HALF_OPEN) {
            state.set(State.CLOSED);
            failureCount.set(0);
            lastStateTransitionTime = System.currentTimeMillis();
            logger.info("Circuit Breaker [{}] transicionado de HALF_OPEN para CLOSED. Servico restabelecido com sucesso.", name);
        } else if (currentState == State.CLOSED) {
            failureCount.set(0);
        }
    }

    public void recordFailure() {
        State currentState = state.get();
        if (currentState == State.CLOSED) {
            int count = failureCount.incrementAndGet();
            logger.warn("Circuit Breaker [{}] registrou falha. Contagem atual: {}/{}", name, count, failureThreshold);
            if (count >= failureThreshold) {
                state.set(State.OPEN);
                lastStateTransitionTime = System.currentTimeMillis();
                logger.error("Circuit Breaker [{}] transicionado para OPEN devido a {} falhas consecutivas.", name, count);
            }
        } else if (currentState == State.HALF_OPEN) {
            state.set(State.OPEN);
            lastStateTransitionTime = System.currentTimeMillis();
            logger.error("Circuit Breaker [{}] falhou em HALF_OPEN. Transicionado de volta para OPEN.", name);
        }
    }

    public State getState() {
        if (state.get() == State.OPEN && (System.currentTimeMillis() - lastStateTransitionTime > retryPeriodMs)) {
            return State.HALF_OPEN;
        }
        return state.get();
    }
}
