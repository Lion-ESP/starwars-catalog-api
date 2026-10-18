package io.github.lionesp.starwarscatalog.application.port.out;

/**
 * The catalog provider answered with an error or with something that cannot be used; waiting will not help.
 */
public abstract class CatalogUpstreamException extends RuntimeException {

	/**
	 * Creates the exception.
	 *
	 * @param message description of the failure
	 * @param cause   underlying cause, may be {@code null}
	 */
	protected CatalogUpstreamException(String message, Throwable cause) {
		super(message, cause);
	}

}
