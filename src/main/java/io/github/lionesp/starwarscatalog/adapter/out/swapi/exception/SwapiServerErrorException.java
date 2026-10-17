package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

/**
 * SWAPI answered with a 5xx status.
 */
public class SwapiServerErrorException extends RuntimeException {

	private final int status;

	/**
	 * Creates the exception.
	 *
	 * @param status HTTP status returned by SWAPI
	 */
	public SwapiServerErrorException(int status) {
		super("SWAPI answered with server error " + status);
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
