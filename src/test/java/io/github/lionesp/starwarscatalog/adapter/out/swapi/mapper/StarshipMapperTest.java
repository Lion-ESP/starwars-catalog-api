package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiListResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiStarshipProperties;
import io.github.lionesp.starwarscatalog.domain.model.Starship;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class StarshipMapperTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final StarshipMapper mapper = new StarshipMapper();

	@Test
	void E31_searchResultEnvelopeMapsToDomain() throws IOException {
		SwapiListResponse<SwapiStarshipProperties> response;
		try (InputStream stream = getClass().getResourceAsStream("/swapi/starships-search.json")) {
			response = JSON.readValue(stream, new TypeReference<>() {
			});
		}

		List<Starship> starships = response.result().stream().map(mapper::toDomain).toList();

		Starship expectedDeathStar = new Starship("9", "Death Star", "DS-1 Orbital Battle Station",
				"Imperial Department of Military Research, Sienar Fleet Systems",
				"Deep Space Mobile Battlestation", 1_000_000_000_000L, 120000.0, 342953L, 843342L,
				1_000_000_000_000L, null, 4.0, 10L, "3 years", List.of(), List.of("1"));
		assertThat(starships).containsExactly(expectedDeathStar);
	}

	@Test
	void E28_rangeAndNoneInNumericFieldsBecomeNull() {
		SwapiStarshipProperties oddValues = new SwapiStarshipProperties("Odd", "m", "mf", "cls", "unknown", "n/a",
				"30-165", "none", "", "x", "?", null, "c", null, null);

		Starship starship = mapper.toDomain(new SwapiItem<>("77", oddValues));

		assertThat(starship.costInCredits()).isNull();
		assertThat(starship.length()).isNull();
		assertThat(starship.crew()).isNull();
		assertThat(starship.passengers()).isNull();
		assertThat(starship.cargoCapacity()).isNull();
		assertThat(starship.maxAtmospheringSpeed()).isNull();
		assertThat(starship.hyperdriveRating()).isNull();
		assertThat(starship.mglt()).isNull();
		assertThat(starship.pilotIds()).isEmpty();
		assertThat(starship.filmIds()).isEmpty();
		assertThat(starship.name()).isEqualTo("Odd");
	}

}
