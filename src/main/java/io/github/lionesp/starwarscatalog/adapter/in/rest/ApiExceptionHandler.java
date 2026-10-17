package io.github.lionesp.starwarscatalog.adapter.in.rest;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into Problem Details responses (RFC 9457).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	static final String INVALID_CREDENTIALS_DETAIL = "Invalid username or password";

	/**
	 * Handles a request body that failed bean validation.
	 *
	 * @param exception validation failure
	 * @return a 400 problem that names every invalid field
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
				.toList();
		String fields = String.join(", ", errors.stream().map(error -> error.get("field")).distinct().toList());
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Invalid value in field: " + fields);
		problem.setTitle("Bad Request");
		problem.setProperty("errors", errors);
		return problem;
	}

	/**
	 * Handles a failed authentication, with the same answer whether the user exists or not.
	 *
	 * @param exception authentication failure
	 * @return a 401 problem with a single generic detail
	 */
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication(AuthenticationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_DETAIL);
		problem.setTitle("Unauthorized");
		return problem;
	}
}
