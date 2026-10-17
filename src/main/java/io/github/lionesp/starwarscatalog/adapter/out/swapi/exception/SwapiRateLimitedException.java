package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import java.time.Duration;

/**
 * SWAPI answered 429 because its usage limit was exhausted.
 */
public class SwapiRateLimitedException extends SwapiUnavailableException {

	/**
	 * Creates the exception.
	 *
	 * @param retryAfter time until the rate limit window resets
	 */
	public SwapiRateLimitedException(Duration retryAfter) {
		super("SWAPI rate limit exceeded", null, retryAfter);
	}

}
