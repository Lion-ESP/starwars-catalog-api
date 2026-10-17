package io.github.lionesp.starwarscatalog.adapter.in.rest;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiBadResponseException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiUnavailableException;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogItemNotFoundException;
import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

	/**
	 * Handles a parameter rejected by a domain rule.
	 *
	 * @param exception the rejected parameter
	 * @return a 400 problem that names the parameter
	 */
	@ExceptionHandler(InvalidParameterException.class)
	ProblemDetail handleInvalidParameter(InvalidParameterException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Invalid parameter '" + exception.parameter() + "': " + exception.getMessage());
		problem.setTitle("Bad Request");
		problem.setProperty("parameter", exception.parameter());
		return problem;
	}

	/**
	 * Handles a parameter that could not be converted to its type, such as a non-numeric page.
	 *
	 * @param exception the failed conversion
	 * @return a 400 problem that names the parameter
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Invalid parameter '" + exception.getName() + "': the value is not valid");
		problem.setTitle("Bad Request");
		problem.setProperty("parameter", exception.getName());
		return problem;
	}

	/**
	 * Handles a lookup of an id that does not exist.
	 *
	 * @param exception the missing item
	 * @return a 404 problem
	 */
	@ExceptionHandler(CatalogItemNotFoundException.class)
	ProblemDetail handleNotFound(CatalogItemNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Not Found");
		return problem;
	}

	/**
	 * Handles a SWAPI failure that is not worth waiting for.
	 *
	 * @param exception the upstream failure
	 * @return a 502 problem with a generic detail
	 */
	@ExceptionHandler({SwapiServerErrorException.class, SwapiBadResponseException.class})
	ProblemDetail handleUpstreamFailure(RuntimeException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
				"The upstream service failed or answered something unexpected");
		problem.setTitle("Bad Gateway");
		return problem;
	}

	/**
	 * Handles a SWAPI outage that the client may retry later.
	 *
	 * @param exception the upstream unavailability
	 * @return a 503 problem with a {@code Retry-After} header in whole seconds
	 */
	@ExceptionHandler(SwapiUnavailableException.class)
	ResponseEntity<ProblemDetail> handleUpstreamUnavailable(SwapiUnavailableException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"The upstream service is temporarily unavailable");
		problem.setTitle("Service Unavailable");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds(exception.retryAfter())))
				.body(problem);
	}

	private static long retryAfterSeconds(Duration retryAfter) {
		long seconds = retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
		return Math.max(1, seconds);
	}
}
