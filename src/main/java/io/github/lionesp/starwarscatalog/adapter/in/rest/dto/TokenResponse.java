package io.github.lionesp.starwarscatalog.adapter.in.rest.dto;

import io.github.lionesp.starwarscatalog.security.TokenService;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Access token returned by a successful login.
 *
 * @param accessToken signed JWT
 * @param tokenType   scheme to use in the {@code Authorization} header, always {@code Bearer}
 * @param expiresIn   lifetime of the token in seconds
 */
@Schema(description = "Access token returned by a successful login")
public record TokenResponse(
		@Schema(description = "Signed JWT to send as a bearer token") String accessToken,
		@Schema(description = "Scheme to use in the Authorization header", example = "Bearer") String tokenType,
		@Schema(description = "Lifetime of the token in seconds", example = "3600") long expiresIn) {

	/** Scheme of the issued tokens. */
	public static final String BEARER = TokenService.BEARER_SCHEME;

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
