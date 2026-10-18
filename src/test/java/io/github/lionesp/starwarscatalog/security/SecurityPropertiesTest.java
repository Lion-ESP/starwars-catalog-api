package io.github.lionesp.starwarscatalog.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class SecurityPropertiesTest {

	private static final String VALID_SECRET = "a".repeat(40);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class)
			.withPropertyValues("app.security.jwt.secret=" + VALID_SECRET);

	@Test
	@DisplayName("a zero expiration fails the startup")
	void zeroExpirationFailsTheStartup() {
		contextRunner.withPropertyValues("app.security.jwt.expiration=0s")
				.run(context -> assertThat(context).hasFailed().getFailure()
						.hasStackTraceContaining("expiration must be positive"));
	}

	@Test
	@DisplayName("a negative expiration fails the startup")
	void negativeExpirationFailsTheStartup() {
		contextRunner.withPropertyValues("app.security.jwt.expiration=-5m")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	@DisplayName("a blank role fails the startup")
	void blankRoleFailsTheStartup() {
		contextRunner.withPropertyValues("app.security.users[0].username=someone",
						"app.security.users[0].password-hash=hash", "app.security.users[0].roles[0]= ")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	@DisplayName("a valid configuration starts")
	void validConfigurationStarts() {
		contextRunner.withPropertyValues("app.security.users[0].username=someone",
						"app.security.users[0].password-hash=hash", "app.security.users[0].roles[0]=USER")
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	@DisplayName("toString of the JWT settings does not print the secret")
	void jwtSettingsToStringMasksTheSecret() {
		JwtProperties properties = new JwtProperties(VALID_SECRET, "issuer", Duration.ofHours(1));

		assertThat(properties.toString()).doesNotContain(VALID_SECRET).contains("issuer");
	}

	@Test
	@DisplayName("toString of a user does not print the password hash")
	void userToStringMasksThePasswordHash() {
		UsersProperties.User user = new UsersProperties.User("someone", "$2a$10$secrethash", List.of("USER"));

		assertThat(user.toString()).doesNotContain("secrethash").contains("someone");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({JwtProperties.class, UsersProperties.class})
	static class PropertiesConfiguration {
	}
}
