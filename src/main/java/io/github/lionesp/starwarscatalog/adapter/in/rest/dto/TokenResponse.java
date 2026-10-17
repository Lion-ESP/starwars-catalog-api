package io.github.lionesp.starwarscatalog.adapter.in.rest.dto;

/**
 * Access token returned by a successful login.
 *
 * @param accessToken signed JWT
 * @param tokenType   scheme to use in the {@code Authorization} header, always {@code Bearer}
 * @param expiresIn   lifetime of the token in seconds
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

	/** Scheme of the issued tokens. */
	public static final String BEARER = "Bearer";

	/**
	 * Builds a response for a bearer token.
	 *
	 * @param accessToken signed JWT
	 * @param expiresIn   lifetime of the token in seconds
	 * @return the response
	 */
	public static TokenResponse bearer(String accessToken, long expiresIn) {
		return new TokenResponse(accessToken, BEARER, expiresIn);
	}
}
