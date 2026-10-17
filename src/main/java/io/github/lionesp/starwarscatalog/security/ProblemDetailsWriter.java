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

	/**
	 * Writes the problem as {@code application/problem+json} with the request URI as instance.
	 *
	 * @param objectMapper serializer for the body
	 * @param request      current request
	 * @param response     response to write to
	 * @param problem      problem to serialize; its status becomes the response status
	 * @throws IOException if the body cannot be written
	 */
	static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
			ProblemDetail problem) throws IOException {
		problem.setInstance(URI.create(request.getRequestURI()));
		response.setStatus(problem.getStatus());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
