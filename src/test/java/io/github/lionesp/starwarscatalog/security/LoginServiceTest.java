package io.github.lionesp.starwarscatalog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(OutputCaptureExtension.class)
class LoginServiceTest {

	private static final String SECRET = "login-service-test-secret-with-more-than-32-bytes";

	private final JwtProperties properties = new JwtProperties(SECRET, "starwars-catalog-api", Duration.ofMinutes(30));
	private final TokenService tokenService = new TokenService(properties, Clock.systemUTC());
	private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
	private final LoginService loginService = new LoginService(authenticationManager, tokenService);

	@Test
	@DisplayName("a successful login issues a token with the role names without prefix")
	void successfulLoginIssuesTokenWithRolesWithoutPrefix() {
		Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated("admin", null,
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN", "FACTOR_PASSWORD"));
		when(authenticationManager.authenticate(any())).thenReturn(authenticated);

		IssuedToken issued = loginService.login("admin", "admin-password");

		var decoded = tokenService.decoder().decode(issued.value());
		assertThat(decoded.getSubject()).isEqualTo("admin");
		assertThat(decoded.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactlyInAnyOrder("USER", "ADMIN");
		assertThat(issued.expiresInSeconds()).isEqualTo(1800);
	}

	@Test
	@DisplayName("bad credentials are rethrown and the username is logged without the password")
	void badCredentialsAreRethrownAndLoggedWithoutThePassword(CapturedOutput output) {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

		assertThatThrownBy(() -> loginService.login("ghost", "very-secret-password"))
				.isInstanceOf(BadCredentialsException.class);

		assertThat(output.getAll()).contains("Login failed for username 'ghost'")
				.doesNotContain("very-secret-password");
	}

	@Test
	@DisplayName("control characters in the username cannot forge log lines")
	void controlCharactersInTheUsernameAreNeutralizedInTheLog(CapturedOutput output) {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

		assertThatThrownBy(() -> loginService.login("ghost\nFORGED LINE", "irrelevant"))
				.isInstanceOf(BadCredentialsException.class);

		assertThat(output.getAll()).contains("ghost_FORGED LINE").doesNotContain("ghost\nFORGED LINE");
	}

	@Test
	@DisplayName("an internal authentication failure is not turned into bad credentials")
	void internalAuthenticationFailureIsPropagatedUntouched() {
		InternalAuthenticationServiceException internalFailure = new InternalAuthenticationServiceException("store down");
		when(authenticationManager.authenticate(any())).thenThrow(internalFailure);

		assertThatThrownBy(() -> loginService.login("user", "user-password")).isSameAs(internalFailure);
	}
}
