package io.github.lionesp.starwarscatalog.application.port.out;

import java.time.Duration;

/**
 * The catalog provider is temporarily unavailable, so the caller may retry after a wait.
 */
public abstract class CatalogUnavailableException extends RuntimeException {

	/** Suggested wait before retrying. */
	private final Duration retryAfter;

	/**
	 * Creates the exception.
	 *
	 * @param message    description of the failure
	 * @param cause      underlying cause, may be {@code null}
	 * @param retryAfter how long the caller should wait before trying again
	 */
	protected CatalogUnavailableException(String message, Throwable cause, Duration retryAfter) {
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
