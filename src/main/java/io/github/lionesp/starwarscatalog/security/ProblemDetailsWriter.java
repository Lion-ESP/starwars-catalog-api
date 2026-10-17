package io.github.lionesp.starwarscatalog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Serializes a {@link ProblemDetail} straight into a servlet response, outside of MVC.
 */
final class ProblemDetailsWriter {

	private ProblemDetailsWriter() {
	}

	static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
			ProblemDetail problem) throws IOException {
		problem.setInstance(URI.create(request.getRequestURI()));
		response.setStatus(problem.getStatus());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
