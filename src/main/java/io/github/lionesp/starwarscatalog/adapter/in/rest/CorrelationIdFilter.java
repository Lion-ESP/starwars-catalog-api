package io.github.lionesp.starwarscatalog.adapter.in.rest;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propagates a correlation id through each request: it reuses a valid {@code X-Request-Id}
 * header or generates a UUID, exposes it in the response header and in the MDC under
 * {@code requestId} so every log line of the request carries it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	/** Name of the request and response header carrying the correlation id. */
	public static final String HEADER_NAME = "X-Request-Id";

	/** MDC key under which the correlation id is published. */
	public static final String MDC_KEY = "requestId";

	private static final String REQUEST_ID_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".REQUEST_ID";
	private static final int MAX_LENGTH = 64;
	private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._-]+");

	/**
	 * Resolves the correlation id, binds it to the MDC and the response, and runs the chain.
	 *
	 * @param request  current request
	 * @param response current response
	 * @param chain    remaining filter chain
	 * @throws ServletException if the chain fails
	 * @throws IOException      if the chain fails
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = resolveForRequest(request);
		response.setHeader(HEADER_NAME, requestId);
		MDC.put(MDC_KEY, requestId);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

	/**
	 * Lets the filter run again on the error dispatch, so the logs written while rendering an error carry the
	 * same id as the response.
	 *
	 * @return {@code false}, always
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	private static String resolveForRequest(HttpServletRequest request) {
		if (request.getAttribute(REQUEST_ID_ATTRIBUTE) instanceof String resolved) {
			return resolved;
		}
		String requestId = resolve(request.getHeader(HEADER_NAME));
		request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		return requestId;
	}

	private static String resolve(String incoming) {
		if (incoming != null && incoming.length() <= MAX_LENGTH && ALLOWED.matcher(incoming).matches()) {
			return incoming;
		}
		return UUID.randomUUID().toString();
	}
}
