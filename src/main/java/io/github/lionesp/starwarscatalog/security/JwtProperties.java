package io.github.lionesp.starwarscatalog.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings bound from {@code app.security.jwt}.
 * <p>
 * The application refuses to start when the signing secret is missing or shorter than
 * {@value #MIN_SECRET_BYTES} bytes, which is the minimum for HS256.
 *
 * @param secret     signing secret, provided through the {@code JWT_SECRET} environment variable
 * @param issuer     value of the {@code iss} claim, also required when validating tokens
 * @param expiration lifetime of an issued token
 */
@Validated
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
		String secret,
		@DefaultValue("starwars-catalog-api") @NotBlank String issuer,
		@DefaultValue("1h") @NotNull Duration expiration) {

	/** Minimum length in bytes of the signing secret. */
	public static final int MIN_SECRET_BYTES = 32;

	/**
	 * Checks that the secret is present and long enough.
	 *
	 * @return {@code true} when the secret has at least {@value #MIN_SECRET_BYTES} bytes
	 */
	@AssertTrue(message = "JWT_SECRET must be set and have at least 32 bytes")
	public boolean isSecretValid() {
		return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= MIN_SECRET_BYTES;
	}
}
