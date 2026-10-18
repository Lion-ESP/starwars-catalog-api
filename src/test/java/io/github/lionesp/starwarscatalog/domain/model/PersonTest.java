package io.github.lionesp.starwarscatalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersonTest {

	private static Person personWithFilms(List<String> filmIds) {
		return new Person("1", "Luke Skywalker", null, null, null, null, null, null, null, null, filmIds, null,
				null);
	}

	@Test
	void filmIdsAreCopiedDefensively() {
		List<String> sourceFilmIds = new ArrayList<>(List.of("1"));
		Person person = personWithFilms(sourceFilmIds);

		sourceFilmIds.add("2");

		assertThat(person.filmIds()).containsExactly("1");
	}

	@Test
	void missingRelationListsBecomeEmpty() {
		Person person = personWithFilms(null);

		assertThat(person.filmIds()).isEmpty();
		assertThat(person.starshipIds()).isEmpty();
		assertThat(person.vehicleIds()).isEmpty();
	}

}
