package io.github.lionesp.starwarscatalog.domain.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import org.junit.jupiter.api.Test;

class PageQueryTest {

	@Test
	void absentParametersFallBackToDefaults() {
		PageQuery query = PageQuery.of(null, null);

		assertThat(query).isEqualTo(new PageQuery(1, 10));
	}

	@Test
	void onlyAbsentParameterFallsBackToDefault() {
		assertThat(PageQuery.of(3, null)).isEqualTo(new PageQuery(3, 10));
		assertThat(PageQuery.of(null, 50)).isEqualTo(new PageQuery(1, 50));
	}

	@Test
	void pageBelowOneIsRejected() {
		assertThatThrownBy(() -> PageQuery.of(0, 10))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("page"));
	}

	@Test
	void sizeBelowOneIsRejected() {
		assertThatThrownBy(() -> PageQuery.of(1, 0))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("size"));
	}

	@Test
	void sizeAboveMaximumIsRejected() {
		assertThatThrownBy(() -> PageQuery.of(1, 101))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("size"));
	}

	@Test
	void constructorEnforcesRanges() {
		assertThatThrownBy(() -> new PageQuery(-5, 10)).isInstanceOf(InvalidParameterException.class);
		assertThatThrownBy(() -> new PageQuery(1, -1)).isInstanceOf(InvalidParameterException.class);
	}

	@Test
	void boundariesAreAccepted() {
		assertThat(PageQuery.of(1, 1)).isEqualTo(new PageQuery(1, 1));
		assertThat(PageQuery.of(1, 100)).isEqualTo(new PageQuery(1, 100));
		assertThat(PageQuery.of(Integer.MAX_VALUE, 100).page()).isEqualTo(Integer.MAX_VALUE);
	}

}
