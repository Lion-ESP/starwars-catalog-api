package io.github.lionesp.starwarscatalog.adapter.in.rest;

import io.github.lionesp.starwarscatalog.adapter.in.rest.dto.LoginRequest;
import io.github.lionesp.starwarscatalog.adapter.in.rest.dto.TokenResponse;
import io.github.lionesp.starwarscatalog.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exchanges user credentials for a JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final String ROLE_PREFIX = "ROLE_";

	private final AuthenticationManager authenticationManager;
	private final TokenService tokenService;

	/**
	 * Creates the controller.
	 *
	 * @param authenticationManager checks the credentials
	 * @param tokenService          issues the token
	 */
	public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
		this.authenticationManager = authenticationManager;
		this.tokenService = tokenService;
	}

	/**
	 * Authenticates a user and issues an access token.
	 *
	 * @param request username and password
	 * @return the signed token, its type and its lifetime in seconds
	 */
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
		var roles = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith(ROLE_PREFIX))
				.map(authority -> authority.substring(ROLE_PREFIX.length()))
				.toList();
		String token = tokenService.issue(authentication.getName(), roles);
		return TokenResponse.bearer(token, tokenService.expiresInSeconds());
	}
}
