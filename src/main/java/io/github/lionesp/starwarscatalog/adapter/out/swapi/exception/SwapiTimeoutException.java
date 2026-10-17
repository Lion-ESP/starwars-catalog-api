package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import java.time.Duration;

/**
 * SWAPI did not answer within the read timeout.
 */
public class SwapiTimeoutException extends SwapiUnavailableException {

	/**
	 * Creates the exception.
	 *
	 * @param cause      underlying timeout
	 * @param retryAfter suggested wait before retrying
	 */
	public SwapiTimeoutException(Throwable cause, Duration retryAfter) {
		super("SWAPI read timed out", cause, retryAfter);
	}

}
