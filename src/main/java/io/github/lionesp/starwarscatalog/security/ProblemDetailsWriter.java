package io.github.lionesp.starwarscatalog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Serializes a {@link ProblemDetail} straight into a servlet response, outside of MVC.
 */
final class ProblemDetailsWriter {

	private ProblemDetailsWriter() {
	}

	private static Optional<URI> instanceOf(HttpServletRequest request) {
		try {
			return Optional.of(URI.create(request.getRequestURI()));
		} catch (IllegalArgumentException | NullPointerException invalidUri) {
			return Optional.empty();
		}
	}

	/**
	 * Writes the problem as {@code application/problem+json} with the request URI as instance when it is a valid URI.
	 *
	 * @param objectMapper serializer for the body
	 * @param request      current request
	 * @param response     response to write to
	 * @param problem      problem to serialize; its status becomes the response status
	 * @throws IOException if the body cannot be written
	 */
	static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
			ProblemDetail problem) throws IOException {
		instanceOf(request).ifPresent(problem::setInstance);
		response.setStatus(problem.getStatus());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
