package io.github.lionesp.starwarscatalog.domain.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageQueryTest {

	@Test
	@DisplayName("E11: missing page and size default to page 1 and size 10")
	void absentParametersFallBackToDefaults() {
		PageQuery query = PageQuery.of(null, null);

		assertThat(query).isEqualTo(new PageQuery(1, 10));
	}

	@Test
	@DisplayName("E11: only the missing parameter takes its default")
	void onlyAbsentParameterFallsBackToDefault() {
		assertThat(PageQuery.of(3, null)).isEqualTo(new PageQuery(3, 10));
		assertThat(PageQuery.of(null, 50)).isEqualTo(new PageQuery(1, 50));
	}

	@Test
	@DisplayName("E13: page below 1 names the page parameter")
	void pageBelowOneIsRejected() {
		assertThatThrownBy(() -> PageQuery.of(0, 10))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("page"));
	}

	@Test
	@DisplayName("E13: size below 1 names the size parameter")
	void sizeBelowOneIsRejected() {
		assertThatThrownBy(() -> PageQuery.of(1, 0))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("size"));
	}

	@Test
	@DisplayName("E13: size above 100 names the size parameter")
	void sizeAboveMaximumIsRejected() {
		assertThatThrownBy(() -> PageQuery.of(1, 101))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("size"));
	}

	@Test
	@DisplayName("E13: the record constructor enforces the same ranges")
	void constructorEnforcesRanges() {
		assertThatThrownBy(() -> new PageQuery(-5, 10)).isInstanceOf(InvalidParameterException.class);
		assertThatThrownBy(() -> new PageQuery(1, -1)).isInstanceOf(InvalidParameterException.class);
	}

	@Test
	@DisplayName("E13: boundaries page 1, size 1, size 100 and a huge page are accepted")
	void boundariesAreAccepted() {
		assertThat(PageQuery.of(1, 1)).isEqualTo(new PageQuery(1, 1));
		assertThat(PageQuery.of(1, 100)).isEqualTo(new PageQuery(1, 100));
		assertThat(PageQuery.of(Integer.MAX_VALUE, 100).page()).isEqualTo(Integer.MAX_VALUE);
	}

}
