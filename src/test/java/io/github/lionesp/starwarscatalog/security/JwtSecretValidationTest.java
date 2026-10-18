package io.github.lionesp.starwarscatalog.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class JwtSecretValidationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class);

	@Test
	void startupFailsWithoutSecret() {
		contextRunner.run(context -> assertThat(context).hasFailed().getFailure()
				.rootCause().hasMessageContaining("JWT_SECRET"));
	}

	@Test
	void startupFailsWithEmptySecret() {
		contextRunner.withPropertyValues("app.security.jwt.secret=")
				.run(context -> assertThat(context).hasFailed().getFailure()
						.hasStackTraceContaining("JWT_SECRET"));
	}

	@Test
	void startupFailsWithSecretShorterThanThirtyTwoBytes() {
		String thirtyOneBytesSecret = "a".repeat(31);

		contextRunner.withPropertyValues("app.security.jwt.secret=" + thirtyOneBytesSecret)
				.run(context -> assertThat(context).hasFailed().getFailure()
						.hasStackTraceContaining("JWT_SECRET"));
	}

	@Test
	void startupSucceedsWithThirtyTwoBytesSecret() {
		String thirtyTwoBytesSecret = "a".repeat(32);

		contextRunner.withPropertyValues("app.security.jwt.secret=" + thirtyTwoBytesSecret)
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JwtProperties.class)
	static class PropertiesConfiguration {
	}
}
