package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * SWAPI client settings bound from {@code swapi}.
 *
 * @param baseUrl           base URL of the SWAPI API
 * @param connectTimeout    maximum time to establish a connection
 * @param readTimeout       maximum time to wait for a response
 * @param defaultRetryAfter wait suggested to callers when SWAPI does not say how long to wait
 * @param maxRetryAfter     upper bound of the wait taken from SWAPI's rate limit reset (its quota window)
 */
@Validated
@ConfigurationProperties("swapi")
public record SwapiProperties(
		@DefaultValue("https://www.swapi.tech/api") @NotBlank String baseUrl,
		@DefaultValue("2s") @NotNull Duration connectTimeout,
		@DefaultValue("5s") @NotNull Duration readTimeout,
		@DefaultValue("30s") @NotNull Duration defaultRetryAfter,
		@DefaultValue("15m") @NotNull Duration maxRetryAfter) {

	/**
	 * Validates that every duration is positive.
	 */
	public SwapiProperties {
		requirePositive("connect-timeout", connectTimeout);
		requirePositive("read-timeout", readTimeout);
		requirePositive("default-retry-after", defaultRetryAfter);
		requirePositive("max-retry-after", maxRetryAfter);
	}

	private static void requirePositive(String name, Duration value) {
		if (value != null && (value.isNegative() || value.isZero())) {
			throw new IllegalArgumentException("swapi." + name + " must be positive but was " + value);
		}
	}

}
