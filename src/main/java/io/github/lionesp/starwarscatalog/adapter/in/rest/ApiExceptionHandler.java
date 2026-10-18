package io.github.lionesp.starwarscatalog.adapter.in.rest;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogUnavailableException;
import io.github.lionesp.starwarscatalog.application.port.out.CatalogUpstreamException;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogItemNotFoundException;
import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import io.github.lionesp.starwarscatalog.security.TokenService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates exceptions into Problem Details responses (RFC 9457), including the ones raised by Spring MVC itself
 * such as an unreadable body, an unsupported method or an unknown route.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
	private static final String INVALID_CREDENTIALS_DETAIL = "Invalid username or password";
	private static final String BAD_REQUEST_TITLE = "Bad Request";

	/**
	 * Handles a request body that failed bean validation.
	 *
	 * @param exception validation failure
	 * @param headers   headers to write to the response
	 * @param status    status to answer
	 * @param request   current request
	 * @return a 400 problem that names every invalid field
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
				.toList();
		String fields = String.join(", ", errors.stream().map(error -> error.get("field")).distinct().toList());
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Invalid value in field: " + fields);
		problem.setTitle(BAD_REQUEST_TITLE);
		problem.setProperty("errors", errors);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	/**
	 * Handles a request parameter that failed method validation, such as a name that is too long.
	 *
	 * @param exception validation failure
	 * @param headers   headers to write to the response
	 * @param status    status to answer
	 * @param request   current request
	 * @return a 400 problem that names the first invalid parameter
	 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		var invalid = exception.getParameterValidationResults().stream()
				.filter(result -> !result.getResolvableErrors().isEmpty())
				.findFirst();
		String parameter = invalid.map(result -> result.getMethodParameter().getParameterName()).orElse("unknown");
		String reason = invalid.flatMap(result -> result.getResolvableErrors().stream().findFirst())
				.map(MessageSourceResolvable::getDefaultMessage).orElse("the value is not valid");
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Invalid parameter '" + parameter + "': " + reason);
		problem.setTitle(BAD_REQUEST_TITLE);
		problem.setProperty("parameter", parameter);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	/**
	 * Handles a failed login, with the same answer whether the user exists or not.
	 *
	 * @param exception authentication failure
	 * @return a 401 problem with a single generic detail and the Bearer challenge
	 */
	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_DETAIL);
		problem.setTitle("Unauthorized");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, TokenService.BEARER_SCHEME)
				.body(problem);
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
		problem.setTitle(BAD_REQUEST_TITLE);
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
		problem.setTitle(BAD_REQUEST_TITLE);
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
	 * Handles a provider failure that is not worth waiting for.
	 *
	 * @param exception the upstream failure
	 * @return a 502 problem with a generic detail
	 */
	@ExceptionHandler(CatalogUpstreamException.class)
	ProblemDetail handleUpstreamFailure(CatalogUpstreamException exception) {
		LOGGER.warn("Answering 502: {}", summarize(exception));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
				"The upstream service failed or answered something unexpected");
		problem.setTitle("Bad Gateway");
		return problem;
	}

	/**
	 * Handles a provider outage that the client may retry later.
	 *
	 * @param exception the upstream unavailability
	 * @return a 503 problem with a {@code Retry-After} header in whole seconds
	 */
	@ExceptionHandler(CatalogUnavailableException.class)
	ResponseEntity<ProblemDetail> handleUpstreamUnavailable(CatalogUnavailableException exception) {
		LOGGER.warn("Answering 503: {}", summarize(exception));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"The upstream service is temporarily unavailable");
		problem.setTitle("Service Unavailable");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds(exception.retryAfter())))
				.body(problem);
	}

	/**
	 * Handles anything that no other handler claims, without leaking internal details to the client.
	 *
	 * @param exception the unexpected failure
	 * @return a 500 problem with a generic detail
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		LOGGER.error("Unexpected error while handling the request", exception);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred");
		problem.setTitle("Internal Server Error");
		return problem;
	}

	private static String summarize(Throwable failure) {
		String summary = failure.getClass().getSimpleName() + ": " + failure.getMessage();
		Throwable cause = failure.getCause();
		return cause == null ? summary : summary + " (cause " + cause.getClass().getSimpleName() + ": "
				+ cause.getMessage() + ")";
	}

	private static long retryAfterSeconds(Duration retryAfter) {
		long seconds = retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
		return Math.max(1, seconds);
	}
}
