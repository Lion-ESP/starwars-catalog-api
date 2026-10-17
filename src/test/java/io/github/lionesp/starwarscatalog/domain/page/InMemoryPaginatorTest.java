package io.github.lionesp.starwarscatalog.domain.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryPaginatorTest {

	private static List<Integer> elements(int count) {
		return IntStream.rangeClosed(1, count).boxed().toList();
	}

	@Test
	@DisplayName("E14: page 2 of 6 elements with size 4 holds the 5th and 6th in received order")
	void lastPartialPageKeepsReceivedOrder() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(6), new PageQuery(2, 4));

		assertThat(page.content()).containsExactly(5, 6);
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.size()).isEqualTo(4);
	}

	@Test
	@DisplayName("E11: the default query returns the first 10 of 82 elements")
	void defaultQueryReturnsFirstTen() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(82), PageQuery.of(null, null));

		assertThat(page.content()).containsExactlyElementsOf(elements(10));
		assertThat(page.totalPages()).isEqualTo(9);
	}

	@Test
	@DisplayName("E12: page 10 of 82 elements with size 10 is empty with real totals")
	void pageBeyondLastIsEmptyWithRealTotals() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(82), new PageQuery(10, 10));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(82);
		assertThat(page.totalPages()).isEqualTo(9);
	}

	@Test
	@DisplayName("E45: page 3 with size 4 over 6 elements is empty with totals 6 and 2")
	void pageThreeSizeFourOverSixElements() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(6), new PageQuery(3, 4));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(2);
	}

	@Test
	@DisplayName("E45: page 3 with size 1 over 2 elements is empty with totals 2 and 2")
	void pageThreeSizeOneOverTwoElements() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(2), new PageQuery(3, 1));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
	}

	@Test
	@DisplayName("E46: page Integer.MAX_VALUE with size 100 never overflows")
	void maximumPageDoesNotOverflow() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(6), new PageQuery(Integer.MAX_VALUE, 100));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(6);
		assertThat(page.totalPages()).isEqualTo(1);
	}

	@Test
	@DisplayName("E47: page 2 of a single element is empty and keeps page 2, total 1 and one page")
	void secondPageOfSingleElement() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(1), new PageQuery(2, 10));

		assertThat(page.content()).isEmpty();
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
	}

	@Test
	@DisplayName("E18: zero elements yield zero total pages")
	void emptyListHasZeroTotalPages() {
		Page<Integer> page = InMemoryPaginator.paginate(List.of(), new PageQuery(1, 10));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isZero();
		assertThat(page.totalPages()).isZero();
	}

	@Test
	@DisplayName("E14: the page content is immutable")
	void contentIsImmutable() {
		Page<Integer> page = InMemoryPaginator.paginate(elements(3), new PageQuery(1, 10));

		assertThatThrownBy(() -> page.content().add(99)).isInstanceOf(UnsupportedOperationException.class);
	}

}
