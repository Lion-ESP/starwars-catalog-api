package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiFilmProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiBadResponseException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiConnectionException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiRateLimitedException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiTimeoutException;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SwapiClientTest {

	private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

	private static final Duration SHORT_READ_TIMEOUT = Duration.ofMillis(300);

	private static final int RESPONSE_DELAY_BEYOND_SHORT_TIMEOUT_MILLIS = 2_000;

	private static final Instant NOW = Instant.ofEpochSecond(1_000_000L);

	private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	private final SwapiClient client = SwapiFixtures.client(wireMock.baseUrl(), READ_TIMEOUT, FIXED_CLOCK);

	@Test
	void notFoundOnByIdReturnsEmptyWithASingleCall() {
		wireMock.stubFor(get(urlEqualTo("/people/9999"))
			.willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));

		var result = client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "9999");

		assertThat(result).isEmpty();
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/9999")));
	}

	@Test
	void rateLimitedMakesASingleCall() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(429)));

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOf(SwapiRateLimitedException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void rateLimitedWithResetHeaderRetryAfterIsSecondsUntilReset() {
		long secondsUntilReset = 45L;
		wireMock.stubFor(get(urlEqualTo("/people?page=1&limit=10&expanded=true")).willReturn(aResponse()
			.withStatus(429)
			.withHeader("x-ratelimit-reset", String.valueOf(NOW.getEpochSecond() + secondsUntilReset))));

		assertThatThrownBy(() -> client.fetchPage(SwapiResource.PEOPLE, SwapiPersonProperties.class, 1, 10))
			.isInstanceOfSatisfying(SwapiRateLimitedException.class,
					exception -> assertThat(exception.retryAfter()).isEqualTo(Duration.ofSeconds(secondsUntilReset)));
	}

	@Test
	void rateLimitedWithResetAlreadyPassedRetryAfterIsAtLeastOneSecond() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse()
			.withStatus(429)
			.withHeader("x-ratelimit-reset", String.valueOf(NOW.getEpochSecond() - 100))));

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOfSatisfying(SwapiRateLimitedException.class,
					exception -> assertThat(exception.retryAfter()).isEqualTo(Duration.ofSeconds(1)));
	}

	@Test
	void rateLimitedWithoutResetHeaderRetryAfterIsTheDefault() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(429)));

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOfSatisfying(SwapiRateLimitedException.class,
					exception -> assertThat(exception.retryAfter()).isEqualTo(SwapiFixtures.DEFAULT_RETRY_AFTER));
	}

	@Test
	void readTimeoutThrowsTimeoutWithDefaultRetryAfter() {
		SwapiClient shortTimeoutClient = SwapiFixtures.client(wireMock.baseUrl(), SHORT_READ_TIMEOUT, FIXED_CLOCK);
		wireMock.stubFor(get(urlEqualTo("/people/1"))
			.willReturn(aResponse().withStatus(200).withFixedDelay(RESPONSE_DELAY_BEYOND_SHORT_TIMEOUT_MILLIS)));

		assertThatThrownBy(() -> shortTimeoutClient.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOfSatisfying(SwapiTimeoutException.class,
					exception -> assertThat(exception.retryAfter()).isEqualTo(SwapiFixtures.DEFAULT_RETRY_AFTER));
	}

	@Test
	void closedPortThrowsConnectionWithDefaultRetryAfter() throws IOException {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}
		SwapiClient unreachableClient = SwapiFixtures.client("http://localhost:" + closedPort, READ_TIMEOUT,
				FIXED_CLOCK);

		assertThatThrownBy(() -> unreachableClient.fetchAll(SwapiResource.FILMS, SwapiFilmProperties.class))
			.isInstanceOfSatisfying(SwapiConnectionException.class,
					exception -> assertThat(exception.retryAfter()).isEqualTo(SwapiFixtures.DEFAULT_RETRY_AFTER));
	}

	@Test
	void serverErrorThrowsServerErrorWithASingleCall() {
		wireMock.stubFor(get(urlEqualTo("/films")).willReturn(aResponse().withStatus(503)));

		assertThatThrownBy(() -> client.fetchAll(SwapiResource.FILMS, SwapiFilmProperties.class))
			.isInstanceOf(SwapiServerErrorException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/films")));
	}

	@Test
	void unexpectedClientErrorThrowsBadResponse() {
		wireMock.stubFor(get(urlEqualTo("/people?name=luke")).willReturn(aResponse().withStatus(400)));

		assertThatThrownBy(() -> client.search(SwapiResource.PEOPLE, SwapiPersonProperties.class, "luke"))
			.isInstanceOf(SwapiBadResponseException.class);
	}

	@Test
	void undecodableBodyThrowsBadResponse() {
		wireMock.stubFor(get(urlEqualTo("/people/1"))
			.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("not json")));

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOf(SwapiBadResponseException.class);
	}

	private static final String PAGE_URL = "/people?page=1&limit=10&expanded=true";

	private static final String PERSON_WITHOUT_UID = "{\"properties\":{\"name\":\"Luke\"}}";

	private static final String PERSON_WITHOUT_PROPERTIES = "{\"uid\":\"1\"}";

	private void stubOk(String url, String body) {
		wireMock.stubFor(get(urlEqualTo(url)).willReturn(
				aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"message\":\"ok\",\"results\":[]}",
			"{\"message\":\"ok\",\"total_records\":1,\"results\":[]}",
			"{\"message\":\"ok\",\"total_records\":1,\"total_pages\":1}",
			"{\"message\":\"ok\",\"total_records\":1,\"total_pages\":1,\"results\":[{\"uid\":\"1\"}]}",
			"{\"message\":\"ok\",\"total_records\":1,\"total_pages\":1,\"results\":[{\"properties\":{}}]}" })
	void pageWithIncompleteEnvelopeOrItemThrowsBadResponse(String incompletePage) {
		stubOk(PAGE_URL, incompletePage);

		assertThatThrownBy(() -> client.fetchPage(SwapiResource.PEOPLE, SwapiPersonProperties.class, 1, 10))
			.isInstanceOf(SwapiBadResponseException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"message\":\"ok\"}", "{\"message\":\"ok\",\"result\":null}" })
	void listWithoutResultThrowsBadResponse(String listWithoutResult) {
		stubOk("/films", listWithoutResult);

		assertThatThrownBy(() -> client.fetchAll(SwapiResource.FILMS, SwapiFilmProperties.class))
			.isInstanceOf(SwapiBadResponseException.class);
	}

	@Test
	void listWithItemMissingUidOrPropertiesThrowsBadResponse() {
		stubOk("/films", "{\"result\":[" + PERSON_WITHOUT_UID + "]}");
		stubOk("/people?name=luke", "{\"result\":[" + PERSON_WITHOUT_PROPERTIES + "]}");

		assertThatThrownBy(() -> client.fetchAll(SwapiResource.FILMS, SwapiFilmProperties.class))
			.isInstanceOf(SwapiBadResponseException.class);
		assertThatThrownBy(() -> client.search(SwapiResource.PEOPLE, SwapiPersonProperties.class, "luke"))
			.isInstanceOf(SwapiBadResponseException.class);
	}

	@Test
	void searchWithoutResultThrowsBadResponse() {
		stubOk("/people?name=luke", "{\"message\":\"ok\"}");

		assertThatThrownBy(() -> client.search(SwapiResource.PEOPLE, SwapiPersonProperties.class, "luke"))
			.isInstanceOf(SwapiBadResponseException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"message\":\"ok\"}", "{\"message\":\"ok\",\"result\":null}",
			"{\"result\":{\"uid\":\"1\"}}", "{\"result\":{\"properties\":{}}}" })
	void detailWithoutUsableResultThrowsBadResponseInsteadOfEmpty(String unusableDetail) {
		stubOk("/people/1", unusableDetail);

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOf(SwapiBadResponseException.class);
	}

	@Test
	void rateLimitResetBeyondTheQuotaWindowIsCappedToTheMaximumRetryAfter() {
		long secondsUntilReset = SwapiFixtures.MAX_RETRY_AFTER.toSeconds() * 4;
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse()
			.withStatus(429)
			.withHeader("x-ratelimit-reset", String.valueOf(NOW.getEpochSecond() + secondsUntilReset))));

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOfSatisfying(SwapiRateLimitedException.class,
					exception -> assertThat(exception.retryAfter()).isEqualTo(SwapiFixtures.MAX_RETRY_AFTER));
	}

	@Test
	void rateLimitResetThatIsNotNumericFallsBackToTheDefaultRetryAfter() {
		wireMock.stubFor(get(urlEqualTo("/people/1"))
			.willReturn(aResponse().withStatus(429).withHeader("x-ratelimit-reset", "tomorrow")));

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOfSatisfying(SwapiRateLimitedException.class,
					exception -> assertThat(exception.retryAfter()).isEqualTo(SwapiFixtures.DEFAULT_RETRY_AFTER));
	}

}
