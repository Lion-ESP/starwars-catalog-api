package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builders of SWAPI client and response bodies, and a reader of the JSON fixtures under {@code swapi/} in the test
 * resources.
 */
public final class SwapiFixtures {

	private static final int UNREACHABLE_CALL_THRESHOLD = 1_000_000;

	static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(30);

	static final Duration MAX_RETRY_AFTER = Duration.ofMinutes(15);

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private SwapiFixtures() {
	}

	static SwapiProperties properties(String baseUrl, Duration readTimeout) {
		return new SwapiProperties(baseUrl, Duration.ofSeconds(1), readTimeout, DEFAULT_RETRY_AFTER,
				MAX_RETRY_AFTER);
	}

	static SwapiClient client(String baseUrl, Duration readTimeout, Clock clock) {
		SwapiProperties properties = properties(baseUrl, readTimeout);
		RestClient restClient = new SwapiClientConfig().swapiRestClient(properties);
		return new SwapiClient(restClient, properties, clock, passThroughResilience());
	}

	static SwapiResilience passThroughResilience() {
		RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
		CircuitBreakerRegistry breakerRegistry = CircuitBreakerRegistry
			.of(CircuitBreakerConfig.custom().minimumNumberOfCalls(UNREACHABLE_CALL_THRESHOLD)
				.slidingWindowSize(UNREACHABLE_CALL_THRESHOLD).build());
		return new SwapiResilience(breakerRegistry, retryRegistry);
	}

	/**
	 * Reads the first item of a fixture, whatever its envelope is.
	 *
	 * @param fixtureName name of the file under {@code swapi/} without the {@code .json} extension
	 * @return the first element of {@code results} or {@code result}, or the {@code result} object itself
	 */
	public static JsonNode firstItem(String fixtureName) {
		JsonNode root = readTree(fixtureName);
		if (root.has("results")) {
			return root.get("results").get(0);
		}
		JsonNode result = root.get("result");
		return result.isArray() ? result.get(0) : result;
	}

	/**
	 * Builds a paged response body ({@code results}) with copies of an item whose uids are 1 to {@code count}.
	 *
	 * @param item         item to copy
	 * @param count        number of items in the page
	 * @param totalRecords value of {@code total_records}
	 * @param totalPages   value of {@code total_pages}
	 * @return the serialized response body
	 */
	public static String page(JsonNode item, int count, int totalRecords, int totalPages) {
		ObjectNode root = MAPPER.createObjectNode();
		root.put("message", "ok");
		root.put("total_records", totalRecords);
		root.put("total_pages", totalPages);
		root.set("results", copies(item, count, null, null));
		return root.toString();
	}

	/**
	 * Builds a list response body ({@code result} array) with copies of an item whose uids are 1 to {@code count}.
	 *
	 * @param item        item to copy
	 * @param count       number of items in the list
	 * @param nameField   property that carries the display text of the item, such as {@code name} or {@code title}
	 * @param valuePrefix prefix of the display text, followed by the position of the item, or {@code null} to keep
	 *                    the original text
	 * @return the serialized response body
	 */
	public static String list(JsonNode item, int count, String nameField, String valuePrefix) {
		ObjectNode root = MAPPER.createObjectNode();
		root.put("message", "ok");
		root.set("result", copies(item, count, nameField, valuePrefix));
		return root.toString();
	}

	/**
	 * Builds a detail response body ({@code result} object).
	 *
	 * @param item item to return
	 * @return the serialized response body
	 */
	public static String detail(JsonNode item) {
		ObjectNode root = MAPPER.createObjectNode();
		root.put("message", "ok");
		root.set("result", item);
		return root.toString();
	}

	/**
	 * Reads and deserializes a fixture.
	 *
	 * @param fixtureName name of the file under {@code swapi/} without the {@code .json} extension
	 * @param type        target type
	 * @param <R>         target type
	 * @return the deserialized fixture
	 */
	public static <R> R readFixture(String fixtureName, TypeReference<R> type) {
		return MAPPER.readValue(readContent(fixtureName), type);
	}

	private static ArrayNode copies(JsonNode item, int count, String nameField, String valuePrefix) {
		ArrayNode array = MAPPER.createArrayNode();
		for (int index = 1; index <= count; index++) {
			ObjectNode copy = (ObjectNode) item.deepCopy();
			copy.put("uid", String.valueOf(index));
			if (valuePrefix != null) {
				((ObjectNode) copy.get("properties")).put(nameField, valuePrefix + index);
			}
			array.add(copy);
		}
		return array;
	}

	private static JsonNode readTree(String fixtureName) {
		return MAPPER.readTree(readContent(fixtureName));
	}

	private static String readContent(String fixtureName) {
		try {
			return new ClassPathResource("swapi/" + fixtureName + ".json").getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

}
