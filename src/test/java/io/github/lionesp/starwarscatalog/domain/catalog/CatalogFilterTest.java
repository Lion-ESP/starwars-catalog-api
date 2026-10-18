package io.github.lionesp.starwarscatalog.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import io.github.lionesp.starwarscatalog.domain.model.Film;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogFilterTest {

	private static Person person(String name) {
		return new Person("1", name, null, null, null, null, null, null, null, null, null, null, null);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "   \t"})
	void blankNameIsAbsent(String blankName) {
		assertThat(CatalogFilter.of(null, blankName).nameFilter()).isEmpty();
	}

	@Test
	void nameIsTrimmed() {
		assertThat(CatalogFilter.of(null, "  sky ").nameFilter()).contains("sky");
	}

	@Test
	void nameMatchesPartiallyIgnoringCase() {
		CatalogFilter filter = CatalogFilter.of("1", "LUKE");

		assertThat(filter.matchesName(person("Luke Skywalker"))).isTrue();
	}

	@Test
	void nameNotContainedDoesNotMatch() {
		CatalogFilter filter = CatalogFilter.of("1", "vader");

		assertThat(filter.matchesName(person("Luke Skywalker"))).isFalse();
	}

	@Test
	void filmMatchesByTitle() {
		Film film = new Film("1", "A New Hope", 4, null, null, null, null, null, null, null, null, null);

		assertThat(CatalogFilter.of(null, "hope").matchesName(film)).isTrue();
	}

	@Test
	void absentNameMatchesAll() {
		assertThat(CatalogFilter.of("1", null).matchesName(person("Luke Skywalker"))).isTrue();
	}

	@Test
	void idIsNormalized() {
		assertThat(CatalogFilter.of(" 007 ", null).idFilter()).contains("7");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  "})
	void blankIdIsAbsent(String blankId) {
		assertThat(CatalogFilter.of(blankId, null).idFilter()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "0", "-1", "1.5", "1a", "+1", "00"})
	void invalidIdNamesParameter(String invalidId) {
		assertThatThrownBy(() -> CatalogFilter.of(invalidId, null))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("id"));
	}

	@Test
	void pathIdUsesSameRule() {
		assertThat(CatalogFilter.validateId("12")).isEqualTo("12");
		assertThatThrownBy(() -> CatalogFilter.validateId("x"))
				.isInstanceOf(InvalidParameterException.class);
	}

}
