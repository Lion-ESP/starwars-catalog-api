package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import java.time.Duration;

/**
 * The circuit breaker guarding SWAPI is open, so the call was rejected without reaching SWAPI.
 */
public class SwapiCircuitOpenException extends SwapiUnavailableException {

	/**
	 * Creates the exception.
	 *
	 * @param cause      the breaker rejection
	 * @param retryAfter the wait the breaker keeps in the open state
	 */
	public SwapiCircuitOpenException(Throwable cause, Duration retryAfter) {
		super("SWAPI circuit breaker is open", cause, retryAfter);
	}

}
