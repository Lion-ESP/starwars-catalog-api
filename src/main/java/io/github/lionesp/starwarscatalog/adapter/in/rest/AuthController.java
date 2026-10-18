package io.github.lionesp.starwarscatalog.adapter.in.rest;

import io.github.lionesp.starwarscatalog.adapter.in.rest.dto.LoginRequest;
import io.github.lionesp.starwarscatalog.adapter.in.rest.dto.TokenResponse;
import io.github.lionesp.starwarscatalog.security.IssuedToken;
import io.github.lionesp.starwarscatalog.security.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exchanges user credentials for a JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Token issuing")
public class AuthController {

	private final LoginService loginService;

	/**
	 * Creates the controller.
	 *
	 * @param loginService checks the credentials and issues the token
	 */
	public AuthController(LoginService loginService) {
		this.loginService = loginService;
	}

	/**
	 * Authenticates a user and issues an access token.
	 *
	 * @param request username and password
	 * @return the signed token, its type and its lifetime in seconds
	 */
	@PostMapping("/login")
	@Operation(summary = "Log in", description = "Exchanges a username and password for a bearer token.")
	@ApiResponse(responseCode = "200", description = "Token issued")
	@ApiResponse(responseCode = "400", description = "Missing, malformed or too long username or password",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Wrong username or password",
			headers = @Header(name = "WWW-Authenticate", description = "Authentication scheme: Bearer",
					schema = @Schema(type = "string")),
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		IssuedToken issued = loginService.login(request.username(), request.password());
		return TokenResponse.bearer(issued.value(), issued.expiresInSeconds());
	}
}
