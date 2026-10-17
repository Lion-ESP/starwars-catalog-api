package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiCircuitOpenException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Runs calls to SWAPI through a retry wrapped by a circuit breaker: {@code CircuitBreaker(Retry(call))}, so a
 * whole sequence of attempts counts once in the breaker.
 */
@Component
public class SwapiResilience {

	private static final String INSTANCE = "swapi";

	private final CircuitBreaker circuitBreaker;

	private final Retry retry;

	/**
	 * Creates the decorator.
	 *
	 * @param circuitBreakerRegistry registry holding the {@code swapi} breaker
	 * @param retryRegistry          registry holding the {@code swapi} retry
	 */
	public SwapiResilience(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
		this.retry = retryRegistry.retry(INSTANCE);
	}

	/**
	 * Executes the call with retry and circuit breaker protection.
	 *
	 * @param call the call to SWAPI
	 * @param <T>  result type
	 * @return the call result
	 * @throws SwapiCircuitOpenException when the breaker is open and rejects the call
	 */
	public <T> T execute(Supplier<T> call) {
		Supplier<T> retried = Retry.decorateSupplier(retry, call);
		Supplier<T> protectedCall = CircuitBreaker.decorateSupplier(circuitBreaker, retried);
		try {
			return protectedCall.get();
		}
		catch (CallNotPermittedException exception) {
			throw new SwapiCircuitOpenException(exception, openStateWait());
		}
	}

	private Duration openStateWait() {
		long millis = circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1);
		return Duration.ofMillis(millis);
	}

}
