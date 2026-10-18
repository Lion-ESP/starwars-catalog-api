package io.github.lionesp.starwarscatalog.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Issues HS256-signed JWTs and builds the matching decoder.
 */
public class TokenService {

	/** Name of the claim that carries the user roles. */
	public static final String ROLES_CLAIM = "roles";

	/** Prefix Spring Security puts on role authorities, for example {@code ROLE_USER}. */
	public static final String ROLE_PREFIX = "ROLE_";

	/** Authentication scheme of the issued tokens, as used in {@code WWW-Authenticate} and {@code Authorization}. */
	public static final String BEARER_SCHEME = "Bearer";

	private final JwtProperties properties;
	private final Clock clock;
	private final SecretKey key;
	private final JwtEncoder encoder;

	/**
	 * Creates the service.
	 *
	 * @param properties JWT settings
	 * @param clock      source of the issue time
	 */
	public TokenService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	/**
	 * Issues a signed token.
	 *
	 * @param subject username the token is issued for
	 * @param roles   roles without the {@code ROLE_} prefix
	 * @return the compact serialized JWT
	 */
	public String issue(String subject, Collection<String> roles) {
		Instant issuedAt = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(subject)
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(properties.expiration()))
				.claim(ROLES_CLAIM, roles)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/**
	 * Returns the lifetime of issued tokens in seconds.
	 *
	 * @return the configured lifetime in seconds
	 */
	public long expiresInSeconds() {
		return properties.expiration().toSeconds();
	}

	/**
	 * Builds a decoder that verifies the HS256 signature, the expiry and the issuer.
	 *
	 * @return a decoder for tokens issued by this service
	 */
	public JwtDecoder decoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}
}
