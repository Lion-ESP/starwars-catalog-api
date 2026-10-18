package io.github.lionesp.starwarscatalog.domain.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InMemoryPaginatorTest {

	private static List<Integer> elements(int count) {
		return IntStream.rangeClosed(1, count).boxed().toList();
	}

	@Test
	void lastPartialPageKeepsReceivedOrder() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(6), new PageQuery(2, 4));

		assertThat(page.content()).containsExactly(5, 6);
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.size()).isEqualTo(4);
	}

	@Test
	void defaultQueryReturnsFirstTen() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(82), PageQuery.of(null, null));

		assertThat(page.content()).containsExactlyElementsOf(elements(10));
		assertThat(page.totalPages()).isEqualTo(9);
	}

	@Test
	void pageBeyondLastIsEmptyWithRealTotals() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(82), new PageQuery(10, 10));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(82);
		assertThat(page.totalPages()).isEqualTo(9);
	}

	@Test
	void pageThreeSizeFourOverSixElements() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(6), new PageQuery(3, 4));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(2);
	}

	@Test
	void pageThreeSizeOneOverTwoElements() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(2), new PageQuery(3, 1));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
	}

	@Test
	void maximumPageDoesNotOverflow() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(6), new PageQuery(Integer.MAX_VALUE, 100));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(1);
	}

	@Test
	void secondPageOfSingleElement() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(1), new PageQuery(2, 10));

		assertThat(page.content()).isEmpty();
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
	}

	@Test
	void emptyListHasZeroTotalPages() {
		Page<Integer> page = InMemoryPaginator.paginate(List.of(), new PageQuery(1, 10));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isZero();
		assertThat(page.totalPages()).isZero();
	}

	@Test
	void contentIsImmutable() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(3), new PageQuery(1, 10));

		assertThatThrownBy(() -> page.content().add(99)).isInstanceOf(UnsupportedOperationException.class);
	}

}
