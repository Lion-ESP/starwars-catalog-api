package io.github.lionesp.testsupport;

import io.github.lionesp.starwarscatalog.security.JwtProperties;
import io.github.lionesp.starwarscatalog.security.TokenService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Builds {@code Authorization} header values for the tests, valid and invalid.
 */
public final class TestTokens {

	private static final String USER_ROLE = "USER";

	private static final String ADMIN_ROLE = "ADMIN";

	private static final String OTHER_SECRET = "some-other-secret-with-more-than-32-bytes";

	private static final Duration EXPIRED_SINCE = Duration.ofHours(2);

	private static final String NOT_A_JWT = "not.a.jwt";

	private TestTokens() {
	}

	/**
	 * Kinds of bearer tokens the resource server must reject.
	 */
	public enum Invalid {

		/** Correctly signed but past its expiration. */
		EXPIRED {
			@Override
			public String token(JwtProperties properties) {
				Clock past = Clock.fixed(Instant.now().minus(EXPIRED_SINCE), ZoneOffset.UTC);
				return new TokenService(properties, past).issue(TestCredentials.USER_USERNAME, List.of(USER_ROLE));
			}
		},

		/** Not signed with the secret of the application. */
		BAD_SIGNATURE {
			@Override
			public String token(JwtProperties properties) {
				JwtProperties otherSecret = new JwtProperties(OTHER_SECRET, properties.issuer(),
						properties.expiration());
				return new TokenService(otherSecret, Clock.systemUTC()).issue(TestCredentials.USER_USERNAME, List.of(USER_ROLE));
			}
		},

		/** Not a JWT at all. */
		MALFORMED {
			@Override
			public String token(JwtProperties properties) {
				return NOT_A_JWT;
			}
		};

		/**
		 * Builds a token of this kind.
		 *
		 * @param properties JWT settings of the application under test
		 * @return the raw token, without the scheme
		 */
		public abstract String token(JwtProperties properties);

		/**
		 * Builds the {@code Authorization} header value of a token of this kind.
		 *
		 * @param properties JWT settings of the application under test
		 * @return the header value, with the Bearer scheme
		 */
		public String bearer(JwtProperties properties) {
			return TestTokens.bearer(token(properties));
		}

	}

	/**
	 * Prefixes a raw token with the Bearer scheme.
	 *
	 * @param token raw token
	 * @return the {@code Authorization} header value
	 */
	public static String bearer(String token) {
		return TokenService.BEARER_SCHEME + " " + token;
	}

	/**
	 * Issues a valid token of the USER demo user.
	 *
	 * @param tokenService service of the application under test
	 * @return the {@code Authorization} header value
	 */
	public static String userBearer(TokenService tokenService) {
		return bearer(tokenService.issue(TestCredentials.USER_USERNAME, List.of(USER_ROLE)));
	}

	/**
	 * Issues a valid token of the ADMIN demo user.
	 *
	 * @param tokenService service of the application under test
	 * @return the {@code Authorization} header value
	 */
	public static String adminBearer(TokenService tokenService) {
		return bearer(tokenService.issue(TestCredentials.ADMIN_USERNAME, List.of(USER_ROLE, ADMIN_ROLE)));
	}

}
