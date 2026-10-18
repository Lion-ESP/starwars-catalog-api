package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiFixtures;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiFilmProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiListResponse;
import io.github.lionesp.starwarscatalog.domain.model.Film;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

class FilmMapperTest {

	private final FilmMapper mapper = new FilmMapper();

	@Test
	void resultListEnvelopeMapsToDomain() {
		SwapiListResponse<SwapiFilmProperties> response = SwapiFixtures.readFixture("films", new TypeReference<>() {
		});

		List<Film> films = response.result().stream().map(mapper::toDomain).toList();

		Film newHope = films.get(0);
		assertThat(films).hasSize(2);
		assertThat(newHope.id()).isEqualTo("1");
		assertThat(newHope.title()).isEqualTo("A New Hope");
		assertThat(newHope.director()).isEqualTo("George Lucas");
		assertThat(newHope.producer()).isEqualTo("Gary Kurtz, Rick McCallum");
		assertThat(newHope.openingCrawl()).startsWith("It is a period of civil war.");
		assertThat(newHope.characterIds()).hasSize(18).startsWith("1", "2");
		assertThat(newHope.planetIds()).containsExactly("1", "2", "3");
		assertThat(newHope.starshipIds()).hasSize(8);
		assertThat(newHope.vehicleIds()).containsExactly("4", "6", "7", "8");
		assertThat(newHope.speciesIds()).hasSize(5);
	}

	@Test
	void releaseDateAndEpisodeAreMapped() {
		SwapiListResponse<SwapiFilmProperties> response = SwapiFixtures.readFixture("films", new TypeReference<>() {
		});

		Film newHope = mapper.toDomain(response.result().get(0));

		assertThat(newHope.releaseDate()).isEqualTo(LocalDate.of(1977, 5, 25));
		assertThat(newHope.episodeId()).isEqualTo(4);
	}

	@Test
	void unparseableReleaseDateBecomesNullAndRestIsMapped() {
		SwapiFilmProperties badDate = new SwapiFilmProperties("Odd", 7, "d", "p", "someday", "crawl", null, null,
				null, null, null);

		Film film = mapper.toDomain(new SwapiItem<>("9", badDate));

		assertThat(film.releaseDate()).isNull();
		assertThat(film.title()).isEqualTo("Odd");
		assertThat(film.episodeId()).isEqualTo(7);
		assertThat(film.characterIds()).isEmpty();
	}

}
