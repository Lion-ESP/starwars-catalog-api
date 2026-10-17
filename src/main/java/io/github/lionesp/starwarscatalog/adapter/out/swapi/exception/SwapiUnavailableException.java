package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import java.time.Duration;

/**
 * Base type for failures that make SWAPI temporarily unusable and that the client may retry after a wait.
 */
public abstract class SwapiUnavailableException extends RuntimeException {

	/** Suggested wait before retrying. */
	private final Duration retryAfter;

	/**
	 * Creates the exception.
	 *
	 * @param message    description of the failure
	 * @param cause      underlying cause, may be {@code null}
	 * @param retryAfter how long the caller should wait before trying again
	 */
	protected SwapiUnavailableException(String message, Throwable cause, Duration retryAfter) {
		super(message, cause);
		this.retryAfter = retryAfter;
	}

	/**
	 * Returns the suggested wait before retrying.
	 *
	 * @return the wait duration
	 */
	public Duration retryAfter() {
		return retryAfter;
	}

}
