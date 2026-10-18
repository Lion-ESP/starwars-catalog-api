package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiFilmProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiStarshipProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiVehicleProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.FilmMapper;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.PersonMapper;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.StarshipMapper;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.VehicleMapper;
import io.github.lionesp.starwarscatalog.domain.model.Film;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import io.github.lionesp.starwarscatalog.domain.model.Starship;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.JsonNode;

class SwapiCatalogAdapterTest {

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	private final SwapiClient client = SwapiFixtures.client(wireMock.baseUrl(), Duration.ofSeconds(2),
			Clock.systemUTC());

	private final SwapiCatalogAdapter<SwapiPersonProperties, Person> peopleAdapter = new SwapiCatalogAdapter<>(
			SwapiResource.PEOPLE, client, new PersonMapper(), SwapiPersonProperties.class);

	private final SwapiCatalogAdapter<SwapiFilmProperties, Film> filmsAdapter = new SwapiCatalogAdapter<>(
			SwapiResource.FILMS, client, new FilmMapper(), SwapiFilmProperties.class);

	private final SwapiCatalogAdapter<SwapiStarshipProperties, Starship> starshipsAdapter = new SwapiCatalogAdapter<>(
			SwapiResource.STARSHIPS, client, new StarshipMapper(), SwapiStarshipProperties.class);

	private final SwapiCatalogAdapter<SwapiVehicleProperties, Vehicle> vehiclesAdapter = new SwapiCatalogAdapter<>(
			SwapiResource.VEHICLES, client, new VehicleMapper(), SwapiVehicleProperties.class);

	private static void stubJson(String url, String body) {
		wireMock.stubFor(get(urlEqualTo(url))
			.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
	}

	@Test
	void secondPageOfPeopleCallsSwapiOnceAndReturnsItsTotals() {
		JsonNode personItem = SwapiFixtures.firstItem("people-page");
		stubJson("/people?page=2&limit=10&expanded=true", SwapiFixtures.page(personItem, 10, 82, 9));

		Page<Person> page = peopleAdapter.findPage(new PageQuery(2, 10));

		assertThat(page.content()).hasSize(10);
		assertThat(page.totalElements()).isEqualTo(82);
		assertThat(page.totalPages()).isEqualTo(9);
		assertThat(page.page()).isEqualTo(2);
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people?page=2&limit=10&expanded=true")));
	}

	@Test
	void pageBeyondTotalPagesReturnsEmptyContentWithTotals() {
		JsonNode personItem = SwapiFixtures.firstItem("people-page");
		stubJson("/people?page=10&limit=10&expanded=true", SwapiFixtures.page(personItem, 2, 82, 9));

		Page<Person> page = peopleAdapter.findPage(new PageQuery(10, 10));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(82);
		assertThat(page.totalPages()).isEqualTo(9);
	}

	@Test
	void secondPageOfFilmsPaginatesInMemoryFromASingleUnpagedCall() {
		JsonNode filmItem = SwapiFixtures.firstItem("films");
		stubJson("/films", SwapiFixtures.list(filmItem, 6, "title", "Film "));

		Page<Film> page = filmsAdapter.findPage(new PageQuery(2, 4));

		assertThat(page.content()).extracting(Film::name).containsExactly("Film 5", "Film 6");
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(2);
		wireMock.verify(1, getRequestedFor(urlEqualTo("/films")));
	}

	@Test
	void pageOfStarshipsAndVehiclesCallsExpandedPageOfEachResource() {
		JsonNode starshipItem = SwapiFixtures.firstItem("starships-search");
		JsonNode vehicleItem = SwapiFixtures.firstItem("vehicle-detail");
		stubJson("/starships?page=2&limit=10&expanded=true", SwapiFixtures.page(starshipItem, 10, 36, 4));
		stubJson("/vehicles?page=2&limit=10&expanded=true", SwapiFixtures.page(vehicleItem, 10, 39, 4));

		Page<Starship> starships = starshipsAdapter.findPage(new PageQuery(2, 10));
		Page<Vehicle> vehicles = vehiclesAdapter.findPage(new PageQuery(2, 10));

		assertThat(starships.content()).hasSize(10);
		assertThat(starships.totalElements()).isEqualTo(36);
		assertThat(vehicles.content()).hasSize(10);
		assertThat(vehicles.totalElements()).isEqualTo(39);
	}

	@Test
	void searchFilmsByNameUsesTheTitleParameter() {
		stubJson("/films?title=hope", SwapiFixtures.list(SwapiFixtures.firstItem("films"), 1, "title", "A New Hope "));

		List<Film> films = filmsAdapter.findAllByName("hope");

		assertThat(films).hasSize(1);
		wireMock.verify(1, getRequestedFor(urlEqualTo("/films?title=hope")));
		wireMock.verify(0, getRequestedFor(urlEqualTo("/films?name=hope")));
	}

	@Test
	void searchWithoutMatchesReturnsEmptyList() {
		stubJson("/people?name=zzz", "{\"message\":\"ok\",\"result\":[]}");

		List<Person> people = peopleAdapter.findAllByName("zzz");

		assertThat(people).isEmpty();
	}

	@Test
	void personByIdCallsDetailAndReturnsThePerson() {
		JsonNode personItem = SwapiFixtures.firstItem("people-page");
		stubJson("/people/1", SwapiFixtures.detail(personItem));

		var person = peopleAdapter.findById("1");

		assertThat(person).isPresent();
		assertThat(person.get().name()).isEqualTo("Luke Skywalker");
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void unknownIdReturnsEmpty() {
		wireMock.stubFor(get(urlEqualTo("/people/9999"))
			.willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));

		var person = peopleAdapter.findById("9999");

		assertThat(person).isEmpty();
	}

}
