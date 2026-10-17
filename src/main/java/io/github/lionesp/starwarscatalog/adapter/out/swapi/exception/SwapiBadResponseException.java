package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

/**
 * SWAPI answered with something this client cannot use: an unexpected 4xx status or an undecodable body.
 */
public class SwapiBadResponseException extends RuntimeException {

	/**
	 * Creates the exception.
	 *
	 * @param message description of the problem
	 * @param cause   underlying cause, may be {@code null}
	 */
	public SwapiBadResponseException(String message, Throwable cause) {
		super(message, cause);
	}

}
