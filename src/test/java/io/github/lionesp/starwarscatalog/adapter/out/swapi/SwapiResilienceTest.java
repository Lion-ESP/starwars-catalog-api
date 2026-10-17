package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiBadResponseException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiCircuitOpenException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiConnectionException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiRateLimitedException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiTimeoutException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.github.lionesp.testsupport.SwapiResilienceTestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = SwapiResilienceTestApplication.class, properties = {
		"swapi.read-timeout=300ms",
		"swapi.connect-timeout=300ms",
		"resilience4j.retry.instances.swapi.wait-duration=10ms" })
class SwapiResilienceTest {

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
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private RetryRegistry retryRegistry;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void resetState() {
		wireMock.resetAll();
		cacheManager.getCache("swapi").clear();
		circuitBreakerRegistry.circuitBreaker("swapi").reset();
	}

	private Object fetchPerson(String id) {
		return client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, id);
	}

	@Test
	void E33_persistentServerError_makesThreeAttemptsThenFails() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(503)));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiServerErrorException.class);

		wireMock.verify(3, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void E34_serverErrorOnceThenOk_returnsTheRecoveredResult() {
		String body = SwapiFixtures.detail(SwapiFixtures.firstItem("people-page"));
		wireMock.stubFor(get(urlEqualTo("/people/1")).inScenario("flaky")
			.whenScenarioStateIs(Scenario.STARTED)
			.willReturn(aResponse().withStatus(500))
			.willSetStateTo("recovered"));
		wireMock.stubFor(get(urlEqualTo("/people/1")).inScenario("flaky")
			.whenScenarioStateIs("recovered")
			.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));

		var result = client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1");

		assertThat(result).isPresent();
		wireMock.verify(2, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void E35_rateLimited_makesASingleCall() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(429)));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiRateLimitedException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void E35_notFound_makesASingleCall() {
		wireMock.stubFor(get(urlEqualTo("/people/9999")).willReturn(aResponse().withStatus(404)));

		client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "9999");

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/9999")));
	}

	@Test
	void E36_readTimeout_isNotRetried() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(200).withFixedDelay(1000)));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiTimeoutException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void badResponse_isNotRetried() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(200)
			.withHeader("Content-Type", "application/json")
			.withBody("not json")));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiBadResponseException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void E51_persistentConnectionRefused_makesThreeAttemptsThenFails() throws IOException {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}
		SwapiProperties closedPortProperties = SwapiFixtures.properties("http://localhost:" + closedPort,
				Duration.ofMillis(300));
		SwapiClient unreachableClient = new SwapiClient(
				new SwapiClientConfig().swapiRestClient(closedPortProperties), closedPortProperties,
				java.time.Clock.systemUTC(), new SwapiResilience(circuitBreakerRegistry, retryRegistry));
		Retry retry = retryRegistry.retry("swapi");
		AtomicInteger retriesPerformed = new AtomicInteger();
		retry.getEventPublisher().onRetry(event -> retriesPerformed.incrementAndGet());

		assertThatThrownBy(() -> unreachableClient.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOf(SwapiConnectionException.class);

		assertThat(retriesPerformed.get()).isEqualTo(2);
	}

	@Test
	void E38_openBreaker_rejectsWithRetryAfterAndDoesNotCallSwapi() {
		circuitBreakerRegistry.circuitBreaker("swapi").transitionToOpenState();

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiCircuitOpenException.class)
			.isInstanceOf(SwapiUnavailableException.class)
			.extracting(exception -> ((SwapiUnavailableException) exception).retryAfter())
			.isEqualTo(Duration.ofSeconds(30));

		wireMock.verify(0, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void E52_cacheHits_areNotRecordedAsBreakerCalls() {
		String body = SwapiFixtures.detail(SwapiFixtures.firstItem("people-page"));
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(
				aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
		CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("swapi");
		fetchPerson("1");
		int bufferedCallsAfterMiss = breaker.getMetrics().getNumberOfBufferedCalls();

		for (int repetition = 0; repetition < 10; repetition++) {
			fetchPerson("1");
		}

		assertThat(bufferedCallsAfterMiss).isEqualTo(1);
		assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(bufferedCallsAfterMiss);
	}

}
