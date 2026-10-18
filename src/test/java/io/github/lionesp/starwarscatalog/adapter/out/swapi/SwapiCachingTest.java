package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lionesp.starwarscatalog.adapter.out.cache.CaffeineCatalogCache;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiBadResponseException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.PersonMapper;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import io.github.lionesp.testsupport.SwapiResilienceTestApplication;
import io.github.lionesp.testsupport.SwapiWireMockSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SwapiResilienceTestApplication.class)
class SwapiCachingTest extends SwapiWireMockSupport {

	@Autowired
	private SwapiClient client;

	@Autowired
	private CaffeineCatalogCache catalogCache;

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
	void sameQueryWithinTtlCallsSwapiOnce() {
		stubPerson("1", 0);

		var firstResult = fetchPerson("1");
		var repeatedResult = fetchPerson("1");

		assertThat(repeatedResult).isEqualTo(firstResult);
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void differentIdsAreCachedSeparately() {
		stubPerson("1", 0);
		stubPerson("2", 0);

		fetchPerson("1");
		fetchPerson("2");

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/2")));
	}

	@Test
	void notFoundTwiceWithinTtlCallsSwapiOnceAndReturnsEmptyBothTimes() {
		wireMock.stubFor(get(urlEqualTo("/people/9999")).willReturn(aResponse().withStatus(404)));

		var firstResult = fetchPerson("9999");
		var repeatedResult = fetchPerson("9999");

		assertThat(firstResult).isEmpty();
		assertThat(repeatedResult).isEmpty();
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/9999")));
	}

	@Test
	void concurrentIdenticalCallsOnEmptyCacheCallSwapiOnce() throws ExecutionException, InterruptedException {
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
	void failuresAreNeverCached() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(503)));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiServerErrorException.class);
		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiServerErrorException.class);

		wireMock.verify(6, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void evictAllMakesTheRepeatedQueryCallSwapiAgain() {
		stubPerson("1", 0);
		fetchPerson("1");

		catalogCache.evictAll();
		fetchPerson("1");

		wireMock.verify(2, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void searchesThatDifferOnlyInCaseShareOneCacheEntry() {
		String body = SwapiFixtures.list(SwapiFixtures.firstItem("people-page"), 1, "name", null);
		wireMock.stubFor(get(urlPathEqualTo("/people")).willReturn(
				aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
		SwapiCatalogAdapter<SwapiPersonProperties, Person> adapter = new SwapiCatalogAdapter<>(SwapiResource.PEOPLE,
				client, new PersonMapper(), SwapiPersonProperties.class);

		var capitalizedSearch = adapter.findAllByName("Luke");
		var lowerCaseSearch = adapter.findAllByName("luke");

		assertThat(lowerCaseSearch).isEqualTo(capitalizedSearch);
		wireMock.verify(1, getRequestedFor(urlPathEqualTo("/people")));
	}

	@Test
	void incompleteEnvelopeIsNotCached() {
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(200)
			.withHeader("Content-Type", "application/json")
			.withBody("{\"message\":\"ok\",\"result\":null}")));

		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiBadResponseException.class);
		assertThatThrownBy(() -> fetchPerson("1")).isInstanceOf(SwapiBadResponseException.class);

		wireMock.verify(2, getRequestedFor(urlEqualTo("/people/1")));
	}

}
