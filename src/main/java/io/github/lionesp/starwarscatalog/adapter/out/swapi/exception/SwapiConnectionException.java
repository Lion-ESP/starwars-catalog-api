package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import java.time.Duration;

/**
 * The connection to SWAPI could not be established.
 */
public class SwapiConnectionException extends SwapiUnavailableException {

	/**
	 * Creates the exception.
	 *
	 * @param cause      underlying connection failure
	 * @param retryAfter suggested wait before retrying
	 */
	public SwapiConnectionException(Throwable cause, Duration retryAfter) {
		super("SWAPI connection failed", cause, retryAfter);
	}

}
