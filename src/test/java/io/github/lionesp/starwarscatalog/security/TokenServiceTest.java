package io.github.lionesp.starwarscatalog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lionesp.testsupport.TestCredentials;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenServiceTest {

	private static final String SECRET = TestCredentials.JWT_SECRET;
	private static final String ISSUER = "starwars-catalog-api";
	private static final Duration LIFETIME = Duration.ofHours(1);

	private final JwtProperties properties = new JwtProperties(SECRET, ISSUER, LIFETIME);

	@Test
	void issuedTokenCarriesTheExpectedClaims() {
		TokenService tokenService = new TokenService(properties, Clock.systemUTC());

		String token = tokenService.issue(TestCredentials.ADMIN_USERNAME, List.of("USER", "ADMIN"));
		Jwt decoded = tokenService.decoder().decode(token);

		assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
		assertThat(decoded.getSubject()).isEqualTo(TestCredentials.ADMIN_USERNAME);
		assertThat(decoded.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactly("USER", "ADMIN");
		assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt())).isEqualTo(LIFETIME);
	}

	@Test
	void expiresInSecondsMatchesConfiguredLifetime() {
		TokenService tokenService = new TokenService(properties, Clock.systemUTC());

		assertThat(tokenService.expiresInSeconds()).isEqualTo(LIFETIME.toSeconds());
	}

	@Test
	void expiredTokenIsRejected() {
		Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
		String expiredToken = new TokenService(properties, twoHoursAgo).issue(TestCredentials.USER_USERNAME, List.of("USER"));

		assertThatThrownBy(() -> new TokenService(properties, Clock.systemUTC()).decoder().decode(expiredToken))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		JwtProperties otherSecret = new JwtProperties("another-secret-with-more-than-32-bytes!", ISSUER,
				LIFETIME);
		String foreignToken = new TokenService(otherSecret, Clock.systemUTC()).issue(TestCredentials.USER_USERNAME, List.of("USER"));

		assertThatThrownBy(() -> new TokenService(properties, Clock.systemUTC()).decoder().decode(foreignToken))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void tokenFromAnotherIssuerIsRejected() {
		JwtProperties otherIssuer = new JwtProperties(SECRET, "someone-else", LIFETIME);
		String foreignToken = new TokenService(otherIssuer, Clock.systemUTC()).issue(TestCredentials.USER_USERNAME, List.of("USER"));

		assertThatThrownBy(() -> new TokenService(properties, Clock.systemUTC()).decoder().decode(foreignToken))
				.isInstanceOf(JwtException.class);
	}
}
