package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogUpstreamException;

/**
 * SWAPI answered with a 5xx status.
 */
public class SwapiServerErrorException extends CatalogUpstreamException {

	/** HTTP status returned by SWAPI. */
	private final int status;

	/**
	 * Creates the exception.
	 *
	 * @param status HTTP status returned by SWAPI
	 */
	public SwapiServerErrorException(int status) {
		super("SWAPI answered with server error " + status, null);
		this.status = status;
	}

	/**
	 * Returns the HTTP status returned by SWAPI.
	 *
	 * @return the status code
	 */
	public int status() {
		return status;
	}

}
