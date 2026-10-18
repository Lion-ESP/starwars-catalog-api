package io.github.lionesp.starwarscatalog.domain.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageTest {

	private static final PageQuery SECOND_PAGE_OF_FOUR = new PageQuery(2, 4);

	@Test
	void ofRoundsTheTotalPagesUp() {
		Page<String> page = Page.of(List.of("e", "f"), SECOND_PAGE_OF_FOUR, 6);

		assertThat(page.content()).containsExactly("e", "f");
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.size()).isEqualTo(4);
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(2);
	}

	@Test
	void ofWithoutElementsHasZeroTotalPages() {
		Page<String> page = Page.of(List.of(), SECOND_PAGE_OF_FOUR, 0);

		assertThat(page.totalElements()).isZero();
		assertThat(page.totalPages()).isZero();
	}

	@Test
	void emptyHasNoContentAndKeepsTheRealTotals() {
		Page<String> page = Page.empty(new PageQuery(10, 10), 82);

		assertThat(page.content()).isEmpty();
		assertThat(page.page()).isEqualTo(10);
		assertThat(page.totalElements()).isEqualTo(82);
		assertThat(page.totalPages()).isEqualTo(9);
	}

	@Test
	void contentIsCopiedDefensively() {
		List<String> sourceContent = new ArrayList<>(List.of("a"));
		Page<String> page = Page.of(sourceContent, SECOND_PAGE_OF_FOUR, 5);

		sourceContent.add("b");

		assertThat(page.content()).containsExactly("a");
	}

	@Test
	void contentIsImmutable() {
		Page<String> page = Page.of(List.of("a"), SECOND_PAGE_OF_FOUR, 5);

		assertThatThrownBy(() -> page.content().add("b")).isInstanceOf(UnsupportedOperationException.class);
	}

}
