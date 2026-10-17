package io.github.lionesp.starwarscatalog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenServiceTest {

	private static final String SECRET = "unit-test-secret-with-more-than-32-bytes";
	private static final String ISSUER = "starwars-catalog-api";

	private final JwtProperties properties = new JwtProperties(SECRET, ISSUER, Duration.ofHours(1));

	@Test
	@DisplayName("E1: el token emitido lleva iss, sub, iat, exp y roles")
	void issuedTokenCarriesTheExpectedClaims() {
		TokenService tokenService = new TokenService(properties);

		String token = tokenService.issue("admin", List.of("USER", "ADMIN"));
		Jwt decoded = tokenService.decoder().decode(token);

		assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
		assertThat(decoded.getSubject()).isEqualTo("admin");
		assertThat(decoded.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactly("USER", "ADMIN");
		assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt())).isEqualTo(Duration.ofHours(1));
	}

	@Test
	@DisplayName("E1: expiresInSeconds refleja la vigencia configurada")
	void expiresInSecondsMatchesConfiguredLifetime() {
		TokenService tokenService = new TokenService(properties);

		assertThat(tokenService.expiresInSeconds()).isEqualTo(3600);
	}

	@Test
	@DisplayName("E5: un token vencido es rechazado")
	void expiredTokenIsRejected() {
		Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
		String expiredToken = new TokenService(properties, twoHoursAgo).issue("user", List.of("USER"));

		assertThatThrownBy(() -> new TokenService(properties).decoder().decode(expiredToken))
				.isInstanceOf(JwtException.class);
	}

	@Test
	@DisplayName("E5: un token firmado con otro secreto es rechazado")
	void tokenSignedWithAnotherSecretIsRejected() {
		JwtProperties otherSecret = new JwtProperties("another-secret-with-more-than-32-bytes!", ISSUER,
				Duration.ofHours(1));
		String foreignToken = new TokenService(otherSecret).issue("user", List.of("USER"));

		assertThatThrownBy(() -> new TokenService(properties).decoder().decode(foreignToken))
				.isInstanceOf(JwtException.class);
	}

	@Test
	@DisplayName("E5: un token de otro emisor es rechazado")
	void tokenFromAnotherIssuerIsRejected() {
		JwtProperties otherIssuer = new JwtProperties(SECRET, "someone-else", Duration.ofHours(1));
		String foreignToken = new TokenService(otherIssuer).issue("user", List.of("USER"));

		assertThatThrownBy(() -> new TokenService(properties).decoder().decode(foreignToken))
				.isInstanceOf(JwtException.class);
	}
}
