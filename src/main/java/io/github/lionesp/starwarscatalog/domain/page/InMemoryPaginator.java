package io.github.lionesp.starwarscatalog.domain.page;

import java.util.List;

/**
 * Slices a complete in-memory list into a {@link Page}, preserving the received order.
 */
public final class InMemoryPaginator {

	private InMemoryPaginator() {
	}

	/**
	 * Returns the requested page of {@code all}. A page past the end is empty but keeps the real totals.
	 *
	 * @param all   complete list, in the order it must be paginated
	 * @param query the requested page
	 * @param <T>   element type
	 * @return the page
	 */
	public static <T> Page<T> paginate(List<T> all, PageQuery query) {
		long offset = ((long) query.page() - 1) * query.size();
		if (offset >= all.size()) {
			return Page.empty(query, all.size());
		}
		int from = (int) offset;
		int to = (int) Math.min((long) from + query.size(), all.size());
		return Page.of(all.subList(from, to), query, all.size());
	}

}
