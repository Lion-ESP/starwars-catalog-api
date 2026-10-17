package io.github.lionesp.starwarscatalog.domain.catalog;

import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import io.github.lionesp.starwarscatalog.domain.model.Film;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogFilterTest {

	private static Person person(String name) {
		return new Person("1", name, null, null, null, null, null, null, null, null, null, null, null);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "   \t"})
	@DisplayName("E19 blank name leaves the name filter absent")
	void e19BlankNameIsAbsent(String blankName) {
		assertThat(CatalogFilter.of(null, blankName).nameFilter()).isEmpty();
	}

	@Test
	@DisplayName("E19 name is trimmed")
	void e19NameIsTrimmed() {
		assertThat(CatalogFilter.of(null, "  sky ").nameFilter()).contains("sky");
	}

	@Test
	@DisplayName("E22 name matches partially and ignoring case")
	void e22NameMatchesPartiallyIgnoringCase() {
		CatalogFilter filter = CatalogFilter.of("1", "LUKE");

		assertThat(filter.matchesName(person("Luke Skywalker"))).isTrue();
	}

	@Test
	@DisplayName("E22 name that is not contained does not match")
	void e22NameNotContainedDoesNotMatch() {
		CatalogFilter filter = CatalogFilter.of("1", "vader");

		assertThat(filter.matchesName(person("Luke Skywalker"))).isFalse();
	}

	@Test
	@DisplayName("E22 film name filter uses the title")
	void e22FilmMatchesByTitle() {
		Film film = new Film("1", "A New Hope", 4, null, null, null, null, null, null, null, null, null);

		assertThat(CatalogFilter.of(null, "hope").matchesName(film)).isTrue();
	}

	@Test
	@DisplayName("E22 absent name filter matches everything")
	void e22AbsentNameMatchesAll() {
		assertThat(CatalogFilter.of("1", null).matchesName(person("Luke Skywalker"))).isTrue();
	}

	@Test
	@DisplayName("E22 id is normalized and exposed")
	void e22IdIsNormalized() {
		assertThat(CatalogFilter.of(" 007 ", null).idFilter()).contains("7");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  "})
	@DisplayName("E23 blank id is absent")
	void e23BlankIdIsAbsent(String blankId) {
		assertThat(CatalogFilter.of(blankId, null).idFilter()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "0", "-1", "1.5", "1a", "+1", "00"})
	@DisplayName("E23 invalid id names the id parameter")
	void e23InvalidIdNamesParameter(String invalidId) {
		assertThatThrownBy(() -> CatalogFilter.of(invalidId, null))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("id"));
	}

	@Test
	@DisplayName("E23 path id uses the same validation")
	void e23PathIdUsesSameRule() {
		assertThat(CatalogFilter.validateId("12")).isEqualTo("12");
		assertThatThrownBy(() -> CatalogFilter.validateId("x"))
				.isInstanceOf(InvalidParameterException.class);
	}

	@Test
	@DisplayName("E25 not found exception carries the searched id")
	void e25NotFoundCarriesId() {
		CatalogItemNotFoundException exception = new CatalogItemNotFoundException("9999");

		assertThat(exception.id()).isEqualTo("9999");
		assertThat(exception.getMessage()).contains("9999");
	}

	@Test
	@DisplayName("E22 records copy lists defensively and null becomes empty")
	void recordsCopyListsDefensively() {
		List<String> source = new ArrayList<>(List.of("1"));
		Person withList = new Person("1", "n", null, null, null, null, null, null, null, null, source, null, null);
		source.add("2");

		assertThat(withList.filmIds()).containsExactly("1");
		assertThat(withList.starshipIds()).isEmpty();
	}

}
