package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.lionesp.starwarscatalog.adapter.out.cache.CaffeineCatalogCache;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = SwapiResilienceTestApplication.class, properties = {
		"resilience4j.retry.instances.swapi.wait-duration=10ms" })
class SwapiCachingTest {

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@DynamicPropertySource
	static void swapiBaseUrl(DynamicPropertyRegistry registry) {
		registry.add("swapi.base-url", wireMock::baseUrl);
	}

	@Autowired
	private SwapiClient client;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private CaffeineCatalogCache catalogCache;

	@BeforeEach
	void resetState() {
		wireMock.resetAll();
		cacheManager.getCache("swapi").clear();
		circuitBreakerRegistry.circuitBreaker("swapi").reset();
	}

	private void stubPerson(String id, int delayMillis) {
		String body = SwapiFixtures.detail(SwapiFixtures.firstItem("people-page"));
		wireMock.stubFor(get(urlEqualTo("/people/" + id)).willReturn(aResponse().withStatus(200)
			.withHeader("Content-Type", "application/json")
			.withFixedDelay(delayMillis)
			.withBody(body)));
	}

	private Optional<SwapiItem<SwapiPersonProperties>> fetchPerson(String id) {
		return client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, id);
	}

	@Test
	void E32_sameQueryWithinTtl_callsSwapiOnce() {
		stubPerson("1", 0);

		var firstResult = fetchPerson("1");
		var repeatedResult = fetchPerson("1");

		assertThat(repeatedResult).isEqualTo(firstResult);
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void E32_differentIds_areCachedSeparately() {
		stubPerson("1", 0);
		stubPerson("2", 0);

		fetchPerson("1");
		fetchPerson("2");

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/2")));
	}

	@Test
	void E49_notFoundTwiceWithinTtl_callsSwapiOnceAndReturnsEmptyBothTimes() {
		wireMock.stubFor(get(urlEqualTo("/people/9999")).willReturn(aResponse().withStatus(404)));

		var firstResult = fetchPerson("9999");
		var repeatedResult = fetchPerson("9999");

		assertThat(firstResult).isEmpty();
		assertThat(repeatedResult).isEmpty();
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/9999")));
	}

	@Test
	void E50_concurrentIdenticalCallsOnEmptyCache_callSwapiOnce() throws ExecutionException, InterruptedException {
		stubPerson("1", 500);
		int concurrentCallers = 8;
		List<Future<Optional<SwapiItem<SwapiPersonProperties>>>> futures = new ArrayList<>();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int caller = 0; caller < concurrentCallers; caller++) {
				futures.add(executor.submit(() -> fetchPerson("1")));
			}
		}

		var firstResult = futures.get(0).get();
		for (Future<Optional<SwapiItem<SwapiPersonProperties>>> future : futures) {
			assertThat(future.get()).isEqualTo(firstResult);
		}
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void failures_areNeverCached() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(503)));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiServerErrorException.class);
		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiServerErrorException.class);

		wireMock.verify(6, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void E8_evictAll_makesTheRepeatedQueryCallSwapiAgain() {
		stubPerson("1", 0);
		fetchPerson("1");

		catalogCache.evictAll();
		fetchPerson("1");

		wireMock.verify(2, getRequestedFor(urlEqualTo("/people/1")));
	}

}
