package io.github.lionesp.starwarscatalog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Answers authenticated requests that lack the required role with a 403 Problem Details body (RFC 9457).
 */
@Component
public class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	/**
	 * Creates the handler.
	 *
	 * @param objectMapper mapper used to serialize the problem
	 */
	public ProblemDetailsAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"The authenticated user is not allowed to access this resource");
		problem.setTitle("Forbidden");
		ProblemDetailsWriter.write(objectMapper, request, response, problem);
	}
}
