package io.github.lionesp.starwarscatalog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Answers unauthenticated requests with a 401 Problem Details body (RFC 9457).
 */
@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	/**
	 * Creates the entry point.
	 *
	 * @param objectMapper mapper used to serialize the problem
	 */
	public ProblemDetailsAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"Authentication is required with a valid bearer token");
		problem.setTitle("Unauthorized");
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, TokenService.BEARER_SCHEME);
		ProblemDetailsWriter.write(objectMapper, request, response, problem);
	}
}
