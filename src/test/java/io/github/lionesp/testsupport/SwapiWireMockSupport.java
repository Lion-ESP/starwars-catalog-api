package io.github.lionesp.testsupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Base of the Spring tests that run the SWAPI client against a WireMock server.
 * <p>
 * The server is one per JVM and is never restarted, so every cached Spring context keeps pointing to a live
 * address. Before each test the stubs, the SWAPI cache and the circuit breaker are reset.
 */
@TestPropertySource(properties = "resilience4j.retry.instances.swapi.wait-duration=10ms")
public abstract class SwapiWireMockSupport {

	/** WireMock server that plays SWAPI. */
	protected static final WireMockServer wireMock = startServer();

	@Autowired
	private CacheManager cacheManager;

	/** Registry that holds the {@code swapi} circuit breaker of the context under test. */
	@Autowired
	protected CircuitBreakerRegistry circuitBreakerRegistry;

	@DynamicPropertySource
	static void swapiBaseUrl(DynamicPropertyRegistry registry) {
		registry.add("swapi.base-url", wireMock::baseUrl);
	}

	@BeforeEach
	void resetState() {
		wireMock.resetAll();
		cacheManager.getCache(SwapiClient.CACHE_NAME).clear();
		circuitBreakerRegistry.circuitBreaker("swapi").reset();
	}

	/**
	 * Stubs a SWAPI response.
	 *
	 * @param url    exact path and query the request must have
	 * @param status HTTP status of the response
	 * @param body   JSON body of the response
	 */
	protected static void stubJson(String url, int status, String body) {
		wireMock.stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(status)
			.withHeader("Content-Type", "application/json")
			.withBody(body)));
	}

	private static WireMockServer startServer() {
		WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
		server.start();
		Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
		return server;
	}

}
