package io.github.lionesp.starwarscatalog.domain.page;

import java.util.List;

/**
 * One page of results together with the totals of the whole result set.
 *
 * @param content       elements of the page, immutable
 * @param page          1-based page number that was requested
 * @param size          page size that was requested
 * @param totalElements number of elements in the whole result set
 * @param totalPages    number of pages needed to hold the whole result set, 0 when it is empty
 * @param <T>           element type
 */
public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	/**
	 * Makes the content immutable.
	 */
	public Page {
		content = List.copyOf(content);
	}

	/**
	 * Builds a page computing {@code totalPages} as {@code ceil(totalElements / size)}.
	 *
	 * @param content       elements of the page
	 * @param query         the request that produced the page
	 * @param totalElements size of the whole result set
	 * @param <T>           element type
	 * @return the page
	 */
	public static <T> Page<T> of(List<T> content, PageQuery query, long totalElements) {
		long pages = (totalElements + query.size() - 1) / query.size();
		return new Page<>(content, query.page(), query.size(), totalElements, (int) pages);
	}

	/**
	 * Builds a page with no content, keeping the real totals.
	 *
	 * @param query         the request that produced the page
	 * @param totalElements size of the whole result set
	 * @param <T>           element type
	 * @return the empty page
	 */
	public static <T> Page<T> empty(PageQuery query, long totalElements) {
		return of(List.of(), query, totalElements);
	}

}
