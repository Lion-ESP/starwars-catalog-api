package io.github.lionesp.starwarscatalog.domain.exception;

/**
 * Signals that a value received from a client violates a domain rule.
 * <p>
 * Carries the name of the offending parameter as exposed by the API, so the web layer
 * can report which input was rejected without parsing the message.
 */
public class InvalidParameterException extends RuntimeException {

	private final String parameter;

	/**
	 * Creates the exception for a rejected parameter.
	 *
	 * @param parameter name of the parameter as exposed by the API, for example {@code size}
	 * @param message   human-readable reason for the rejection
	 */
	public InvalidParameterException(String parameter, String message) {
		super(message);
		this.parameter = parameter;
	}

	/**
	 * Returns the name of the rejected parameter.
	 *
	 * @return the parameter name as exposed by the API
	 */
	public String parameter() {
		return parameter;
	}

}
