package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SwapiPropertiesTest {

	private static final Duration POSITIVE = Duration.ofSeconds(1);

	@Test
	void positiveDurationsAreAccepted() {
		SwapiProperties properties = new SwapiProperties("http://localhost", POSITIVE, POSITIVE, POSITIVE, POSITIVE);

		assertThat(properties.maxRetryAfter()).isEqualTo(POSITIVE);
	}

	@Test
	void zeroOrNegativeDurationsAreRejected() {
		assertThatThrownBy(() -> new SwapiProperties("http://localhost", Duration.ZERO, POSITIVE, POSITIVE, POSITIVE))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("connect-timeout");
		assertThatThrownBy(
				() -> new SwapiProperties("http://localhost", POSITIVE, POSITIVE, POSITIVE, Duration.ofSeconds(-5)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("max-retry-after");
	}

}
