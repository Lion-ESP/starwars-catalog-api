package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiBadResponseException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiCircuitOpenException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiConnectionException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiRateLimitedException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiUnavailableException;
import io.github.lionesp.testsupport.SwapiResilienceTestApplication;
import io.github.lionesp.testsupport.SwapiWireMockSupport;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SwapiResilienceTestApplication.class)
class SwapiResilienceTest extends SwapiWireMockSupport {

	private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(2);

	@Autowired
	private SwapiClient client;

	@Autowired
	private RetryRegistry retryRegistry;

	private Object fetchPerson(String id) {
		return client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, id);
	}

	@Test
	void persistentServerErrorMakesThreeAttemptsThenFails() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(503)));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiServerErrorException.class);

		wireMock.verify(3, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void serverErrorOnceThenOkReturnsTheRecoveredResult() {
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
	void rateLimitedMakesASingleCall() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(429)));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiRateLimitedException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void notFoundMakesASingleCall() {
		wireMock.stubFor(get(urlEqualTo("/people/9999")).willReturn(aResponse().withStatus(404)));

		client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "9999");

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/9999")));
	}

	@Test
	void badResponseIsNotRetried() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(200)
			.withHeader("Content-Type", "application/json")
			.withBody("not json")));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiBadResponseException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void persistentConnectionRefusedMakesThreeAttemptsThenFails() throws IOException {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}
		SwapiProperties closedPortProperties = SwapiFixtures.properties("http://localhost:" + closedPort,
				CLIENT_TIMEOUT);
		SwapiClient unreachableClient = new SwapiClient(
				new SwapiClientConfig().swapiRestClient(closedPortProperties), closedPortProperties,
				Clock.systemUTC(), new SwapiResilience(circuitBreakerRegistry, retryRegistry));
		Retry retry = retryRegistry.retry("swapi");
		AtomicInteger retriesPerformed = new AtomicInteger();
		retry.getEventPublisher().onRetry(event -> retriesPerformed.incrementAndGet());

		assertThatThrownBy(() -> unreachableClient.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOf(SwapiConnectionException.class);

		assertThat(retriesPerformed.get()).isEqualTo(2);
	}

	@Test
	void openBreakerRejectsWithRetryAfterAndDoesNotCallSwapi() {
		circuitBreakerRegistry.circuitBreaker("swapi").transitionToOpenState();

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiCircuitOpenException.class)
			.isInstanceOf(SwapiUnavailableException.class)
			.extracting(exception -> ((SwapiUnavailableException) exception).retryAfter())
			.isEqualTo(Duration.ofSeconds(30));

		wireMock.verify(0, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void cacheHitsAreNotRecordedAsBreakerCalls() {
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

	@Test
	void rateLimitedResponsesAreNotRecordedByTheCircuitBreaker() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(429)));
		CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("swapi");
		int bufferedCallsBefore = breaker.getMetrics().getNumberOfBufferedCalls();

		for (int attempt = 0; attempt < 6; attempt++) {
			assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiRateLimitedException.class);
		}

		assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(bufferedCallsBefore);
		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
	}

}
