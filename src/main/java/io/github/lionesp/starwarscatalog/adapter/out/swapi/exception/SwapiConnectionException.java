package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import java.time.Duration;

/**
 * SWAPI could not be reached: the connection was refused or failed, or any other transport failure that is not
 * a timeout. It is the fallback for transport errors.
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
