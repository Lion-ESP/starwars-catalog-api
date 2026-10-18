package io.github.lionesp.starwarscatalog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lionesp.testsupport.TestCredentials;
import java.time.Clock;
import java.time.Duration;
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

	private final JwtProperties properties = new JwtProperties(TestCredentials.JWT_SECRET, "starwars-catalog-api",
			Duration.ofMinutes(30));
	private final TokenService tokenService = new TokenService(properties, Clock.systemUTC());
	private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
	private final LoginService loginService = new LoginService(authenticationManager, tokenService);

	@Test
	void successfulLoginIssuesTokenWithRolesWithoutPrefix() {
		Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(TestCredentials.ADMIN_USERNAME, null,
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN", "FACTOR_PASSWORD"));
		when(authenticationManager.authenticate(any())).thenReturn(authenticated);

		IssuedToken issued = loginService.login(TestCredentials.ADMIN_USERNAME, TestCredentials.ADMIN_PASSWORD);

		var decoded = tokenService.decoder().decode(issued.value());
		assertThat(decoded.getSubject()).isEqualTo(TestCredentials.ADMIN_USERNAME);
		assertThat(decoded.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactlyInAnyOrder("USER", "ADMIN");
		assertThat(issued.expiresInSeconds()).isEqualTo(Duration.ofMinutes(30).toSeconds());
	}

	@Test
	void badCredentialsAreRethrownAndLoggedWithoutThePassword(CapturedOutput output) {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

		assertThatThrownBy(() -> loginService.login("ghost", "very-secret-password"))
				.isInstanceOf(BadCredentialsException.class);

		assertThat(output.getAll()).contains("Login failed for username 'ghost'")
				.doesNotContain("very-secret-password");
	}

	@Test
	void controlCharactersInTheUsernameAreNeutralizedInTheLog(CapturedOutput output) {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

		assertThatThrownBy(() -> loginService.login("ghost\nFORGED LINE", "irrelevant"))
				.isInstanceOf(BadCredentialsException.class);

		assertThat(output.getAll()).contains("ghost_FORGED LINE").doesNotContain("ghost\nFORGED LINE");
	}

	@Test
	void internalAuthenticationFailureIsPropagatedUntouched() {
		InternalAuthenticationServiceException internalFailure = new InternalAuthenticationServiceException("store down");
		when(authenticationManager.authenticate(any())).thenThrow(internalFailure);

		assertThatThrownBy(() -> loginService.login(TestCredentials.USER_USERNAME, TestCredentials.USER_PASSWORD)).isSameAs(internalFailure);
	}
}
