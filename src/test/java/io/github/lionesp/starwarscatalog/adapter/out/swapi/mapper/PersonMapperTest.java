package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiFixtures;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPageResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

class PersonMapperTest {

	private final PersonMapper mapper = new PersonMapper();

	@Test
	void pagedResultsEnvelopeMapsToDomain() {
		SwapiPageResponse<SwapiPersonProperties> response = SwapiFixtures.readFixture("people-page", new TypeReference<>() {
		});

		List<Person> people = response.results().stream().map(mapper::toDomain).toList();

		Person expectedLuke = new Person("1", "Luke Skywalker", "male", "19BBY", 172L, 77.0, "blond", "fair", "blue",
				"1", List.of("1", "2", "3", "6"), List.of("12", "22"), List.of("14", "30"));
		Person expectedThreepio = new Person("2", "C-3PO", "n/a", "112BBY", 167L, 75.0, "n/a", "gold", "yellow", "1",
				List.of("1", "2", "3", "4", "5", "6"), List.of(), List.of());
		assertThat(response.totalRecords()).isEqualTo(82);
		assertThat(people).containsExactly(expectedLuke, expectedThreepio);
	}

	@Test
	void missingRelationsMapToEmptyListsAndNullHomeworld() {
		SwapiPersonProperties noRelations = new SwapiPersonProperties("Ghost", null, null, "unknown", "n/a", null,
				null, null, null, null, null, null);

		Person person = mapper.toDomain(new SwapiItem<>("99", noRelations));

		assertThat(person.homeworldId()).isNull();
		assertThat(person.filmIds()).isEmpty();
		assertThat(person.starshipIds()).isEmpty();
		assertThat(person.vehicleIds()).isEmpty();
	}

	@Test
	void unknownHeightAndMassBecomeNull() {
		SwapiPersonProperties unknownMeasures = new SwapiPersonProperties("Ghost", null, null, "unknown", "n/a",
				null, null, null, null, List.of(), List.of(), List.of());

		Person person = mapper.toDomain(new SwapiItem<>("99", unknownMeasures));

		assertThat(person.height()).isNull();
		assertThat(person.mass()).isNull();
		assertThat(person.name()).isEqualTo("Ghost");
	}

}
