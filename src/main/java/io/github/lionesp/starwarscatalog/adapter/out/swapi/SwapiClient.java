package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItemResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiListResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPageResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiBadResponseException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiConnectionException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiRateLimitedException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiTimeoutException;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Raw HTTP client for SWAPI: returns the provider DTOs and translates transport and status failures into
 * the {@code Swapi*Exception} family. A 200 whose envelope or items are incomplete is a
 * {@link SwapiBadResponseException}, thrown before the cached method returns so it is never cached. Results are
 * cached in the {@value #CACHE_NAME} cache, outside the retry and
 * circuit breaker, so a cache hit never reaches SWAPI nor the breaker.
 */
@Component
public class SwapiClient {

	/** Name of the Spring cache that holds the SWAPI results. */
	public static final String CACHE_NAME = "swapi";

	private static final String CACHE_KEY = "#root.methodName + '|' + T(java.util.Arrays).toString(#root.args)";

	private static final String RATE_LIMIT_RESET_HEADER = "x-ratelimit-reset";

	private static final Duration MIN_RETRY_AFTER = Duration.ofSeconds(1);

	private final RestClient restClient;

	private final SwapiProperties properties;

	private final Clock clock;

	private final SwapiResilience resilience;

	/**
	 * Creates the client.
	 *
	 * @param restClient the HTTP client already pointing at SWAPI
	 * @param properties SWAPI settings
	 * @param clock      clock used to compute {@code Retry-After}
	 * @param resilience retry and circuit breaker applied to every call that misses the cache
	 */
	public SwapiClient(RestClient restClient, SwapiProperties properties, Clock clock, SwapiResilience resilience) {
		this.restClient = restClient;
		this.properties = properties;
		this.clock = clock;
		this.resilience = resilience;
	}

	/**
	 * Fetches one expanded page of a paginated resource.
	 *
	 * @param resource       the resource to list
	 * @param propertiesType type of the properties of each item
	 * @param page           1-based page number
	 * @param limit          page size
	 * @param <P>            properties type
	 * @return the SWAPI page
	 */
	@Cacheable(cacheNames = CACHE_NAME, sync = true, key = CACHE_KEY)
	public <P> SwapiPageResponse<P> fetchPage(SwapiResource resource, Class<P> propertiesType, int page, int limit) {
		SwapiPageResponse<P> response = this.<SwapiPageResponse<P>>execute(
				responseType(SwapiPageResponse.class, propertiesType),
				"/{resource}?page={page}&limit={limit}&expanded=true", resource.path(), page, limit)
			.orElseThrow(() -> unexpectedNotFound(resource));
		if (response.totalRecords() == null || response.totalPages() == null) {
			throw new SwapiBadResponseException("SWAPI page without total_records or total_pages");
		}
		requireItems(response.results(), "results");
		return response;
	}

	/**
	 * Fetches every item of a resource SWAPI does not paginate.
	 *
	 * @param resource       the resource to list
	 * @param propertiesType type of the properties of each item
	 * @param <P>            properties type
	 * @return all the items
	 */
	@Cacheable(cacheNames = CACHE_NAME, sync = true, key = CACHE_KEY)
	public <P> List<SwapiItem<P>> fetchAll(SwapiResource resource, Class<P> propertiesType) {
		SwapiListResponse<P> response = this.<SwapiListResponse<P>>execute(responseType(SwapiListResponse.class, propertiesType),
				"/{resource}", resource.path())
			.orElseThrow(() -> unexpectedNotFound(resource));
		return itemsOf(response);
	}

	/**
	 * Searches a resource by name using the search parameter that SWAPI expects for it.
	 *
	 * @param resource       the resource to search
	 * @param propertiesType type of the properties of each item
	 * @param name           text to look for
	 * @param <P>            properties type
	 * @return the matching items, possibly empty
	 */
	@Cacheable(cacheNames = CACHE_NAME, sync = true, key = CACHE_KEY)
	public <P> List<SwapiItem<P>> search(SwapiResource resource, Class<P> propertiesType, String name) {
		SwapiListResponse<P> response = this.<SwapiListResponse<P>>execute(responseType(SwapiListResponse.class, propertiesType),
				"/{resource}?{param}={name}", resource.path(), resource.searchParam(), name)
			.orElseThrow(() -> unexpectedNotFound(resource));
		return itemsOf(response);
	}

	/**
	 * Fetches one item by id.
	 *
	 * @param resource       the resource to read
	 * @param propertiesType type of the properties of the item
	 * @param id             SWAPI id
	 * @param <P>            properties type
	 * @return the item, or empty when SWAPI answers 404
	 */
	@Cacheable(cacheNames = CACHE_NAME, sync = true, key = CACHE_KEY)
	public <P> Optional<SwapiItem<P>> fetchById(SwapiResource resource, Class<P> propertiesType, String id) {
		return this.<SwapiItemResponse<P>>execute(responseType(SwapiItemResponse.class, propertiesType),
				"/{resource}/{id}", resource.path(), id)
			.map(response -> requireComplete(response.result()));
	}

	private static <P> List<SwapiItem<P>> itemsOf(SwapiListResponse<P> response) {
		requireItems(response.result(), "result");
		return response.result();
	}

	private static <P> void requireItems(List<SwapiItem<P>> items, String field) {
		if (items == null) {
			throw new SwapiBadResponseException("SWAPI answered without '" + field + "'");
		}
		items.forEach(SwapiClient::requireComplete);
	}

	private static <P> SwapiItem<P> requireComplete(SwapiItem<P> item) {
		if (item == null || item.uid() == null || item.properties() == null) {
			throw new SwapiBadResponseException("SWAPI answered an item without uid or properties");
		}
		return item;
	}

	private static ParameterizedTypeReference<?> rawResponseType(Class<?> envelope, Class<?> propertiesType) {
		return ParameterizedTypeReference
			.forType(ResolvableType.forClassWithGenerics(envelope, propertiesType).getType());
	}

	@SuppressWarnings("unchecked")
	private static <B> ParameterizedTypeReference<B> responseType(Class<?> envelope, Class<?> propertiesType) {
		return (ParameterizedTypeReference<B>) rawResponseType(envelope, propertiesType);
	}

	private static SwapiBadResponseException unexpectedNotFound(SwapiResource resource) {
		return new SwapiBadResponseException("SWAPI answered 404 for the " + resource.path() + " collection");
	}

	private <B> Optional<B> execute(ParameterizedTypeReference<B> type, String template, Object... variables) {
		return resilience.execute(() -> call(type, template, variables));
	}

	private <B> Optional<B> call(ParameterizedTypeReference<B> type, String template, Object... variables) {
		try {
			return restClient.get().uri(template, variables).exchange((request, response) -> {
				HttpStatusCode status = response.getStatusCode();
				if (status.is2xxSuccessful()) {
					B body = response.bodyTo(type);
					if (body == null) {
						throw new SwapiBadResponseException("SWAPI answered with an empty body");
					}
					return Optional.of(body);
				}
				if (status.value() == HttpStatus.NOT_FOUND.value()) {
					return Optional.<B>empty();
				}
				if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
					throw new SwapiRateLimitedException(retryAfterFrom(response.getHeaders().getFirst(RATE_LIMIT_RESET_HEADER)));
				}
				if (status.is5xxServerError()) {
					throw new SwapiServerErrorException(status.value());
				}
				throw new SwapiBadResponseException("SWAPI answered with unexpected status " + status.value());
			});
		}
		catch (ResourceAccessException exception) {
			throw translateTransportFailure(exception);
		}
		catch (RestClientException | HttpMessageNotReadableException exception) {
			throw new SwapiBadResponseException("SWAPI answered with a body that could not be decoded", exception);
		}
	}

	private RuntimeException translateTransportFailure(ResourceAccessException exception) {
		Duration retryAfter = properties.defaultRetryAfter();
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof HttpConnectTimeoutException || cause instanceof ConnectException) {
				return new SwapiConnectionException(exception, retryAfter);
			}
			if (cause instanceof HttpTimeoutException) {
				return new SwapiTimeoutException(exception, retryAfter);
			}
		}
		return new SwapiConnectionException(exception, retryAfter);
	}

	private Duration retryAfterFrom(String resetHeader) {
		if (resetHeader == null) {
			return properties.defaultRetryAfter();
		}
		try {
			long secondsUntilReset = Long.parseLong(resetHeader.trim()) - clock.instant().getEpochSecond();
			Duration wait = Duration.ofSeconds(secondsUntilReset);
			if (wait.compareTo(MIN_RETRY_AFTER) < 0) {
				return MIN_RETRY_AFTER;
			}
			return wait.compareTo(properties.maxRetryAfter()) > 0 ? properties.maxRetryAfter() : wait;
		}
		catch (NumberFormatException exception) {
			return properties.defaultRetryAfter();
		}
	}

}
