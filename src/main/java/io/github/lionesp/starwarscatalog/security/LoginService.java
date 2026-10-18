package io.github.lionesp.starwarscatalog.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Checks user credentials and issues the access token.
 */
public class LoginService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

	private final AuthenticationManager authenticationManager;
	private final TokenService tokenService;

	/**
	 * Creates the service.
	 *
	 * @param authenticationManager checks the credentials
	 * @param tokenService          issues the token
	 */
	public LoginService(AuthenticationManager authenticationManager, TokenService tokenService) {
		this.authenticationManager = authenticationManager;
		this.tokenService = tokenService;
	}

	/**
	 * Authenticates a user and issues a token carrying their roles.
	 *
	 * @param username login name
	 * @param password plain-text password
	 * @return the issued token
	 * @throws BadCredentialsException if the username or the password are wrong
	 */
	public IssuedToken login(String username, String password) {
		Authentication authentication;
		try {
			authentication = authenticationManager
					.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
		} catch (BadCredentialsException failure) {
			LOGGER.info("Login failed for username '{}'", username.replaceAll("\\p{Cntrl}", "_"));
			throw failure;
		}
		var roles = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith(TokenService.ROLE_PREFIX))
				.map(authority -> authority.substring(TokenService.ROLE_PREFIX.length()))
				.toList();
		String token = tokenService.issue(authentication.getName(), roles);
		return new IssuedToken(token, tokenService.expiresInSeconds());
	}
}
