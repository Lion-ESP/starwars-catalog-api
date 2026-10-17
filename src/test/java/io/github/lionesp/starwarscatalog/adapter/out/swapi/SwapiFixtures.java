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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class SwapiFixtures {

	static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(30);

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private SwapiFixtures() {
	}

	static SwapiProperties properties(String baseUrl, Duration readTimeout) {
		return new SwapiProperties(baseUrl, Duration.ofSeconds(1), readTimeout, DEFAULT_RETRY_AFTER);
	}

	static SwapiClient client(String baseUrl, Duration readTimeout, Clock clock) {
		SwapiProperties properties = properties(baseUrl, readTimeout);
		RestClient restClient = new SwapiClientConfig().swapiRestClient(properties);
		return new SwapiClient(restClient, properties, clock, passThroughResilience());
	}

	static SwapiResilience passThroughResilience() {
		RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
		CircuitBreakerRegistry breakerRegistry = CircuitBreakerRegistry
			.of(CircuitBreakerConfig.custom().minimumNumberOfCalls(1_000_000).slidingWindowSize(1_000_000).build());
		return new SwapiResilience(breakerRegistry, retryRegistry);
	}

	public static JsonNode firstItem(String fixtureName) {
		JsonNode root = read(fixtureName);
		if (root.has("results")) {
			return root.get("results").get(0);
		}
		JsonNode result = root.get("result");
		return result.isArray() ? result.get(0) : result;
	}

	public static String page(JsonNode item, int count, int totalRecords, int totalPages) {
		ObjectNode root = MAPPER.createObjectNode();
		root.put("message", "ok");
		root.put("total_records", totalRecords);
		root.put("total_pages", totalPages);
		root.set("results", copies(item, count, null));
		return root.toString();
	}

	public static String list(JsonNode item, int count, String namePrefix) {
		ObjectNode root = MAPPER.createObjectNode();
		root.put("message", "ok");
		root.set("result", copies(item, count, namePrefix));
		return root.toString();
	}

	public static String detail(JsonNode item) {
		ObjectNode root = MAPPER.createObjectNode();
		root.put("message", "ok");
		root.set("result", item);
		return root.toString();
	}

	private static ArrayNode copies(JsonNode item, int count, String namePrefix) {
		ArrayNode array = MAPPER.createArrayNode();
		for (int index = 1; index <= count; index++) {
			ObjectNode copy = (ObjectNode) item.deepCopy();
			copy.put("uid", String.valueOf(index));
			if (namePrefix != null) {
				((ObjectNode) copy.get("properties")).put("title", namePrefix + index);
			}
			array.add(copy);
		}
		return array;
	}

	private static JsonNode read(String fixtureName) {
		try {
			String content = new ClassPathResource("swapi/" + fixtureName + ".json")
				.getContentAsString(StandardCharsets.UTF_8);
			return MAPPER.readTree(content);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

}
