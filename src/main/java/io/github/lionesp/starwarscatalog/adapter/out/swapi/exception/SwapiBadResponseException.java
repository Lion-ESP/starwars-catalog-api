package io.github.lionesp.starwarscatalog.adapter.out.swapi.exception;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogUpstreamException;

/**
 * SWAPI answered with something this client cannot use: an unexpected status (a 4xx other than 404 and 429,
 * or a 404 on a collection), an empty or undecodable body, or a 200 whose envelope or items lack required
 * fields ({@code results}, {@code result}, {@code total_records}, {@code total_pages}, {@code uid} or
 * {@code properties}).
 */
public class SwapiBadResponseException extends CatalogUpstreamException {

	/**
	 * Creates the exception.
	 *
	 * @param message description of the problem
	 */
	public SwapiBadResponseException(String message) {
		super(message, null);
	}

	/**
	 * Creates the exception.
	 *
	 * @param message description of the problem
	 * @param cause   underlying cause
	 */
	public SwapiBadResponseException(String message, Throwable cause) {
		super(message, cause);
	}

}
