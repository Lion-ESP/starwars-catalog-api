package io.github.lionesp.starwarscatalog.adapter.in.rest.dto;

import io.github.lionesp.starwarscatalog.domain.page.Page;
import java.util.List;

/**
 * Paginated response body of the catalog listings.
 *
 * @param content       elements of the requested page
 * @param page          1-based page number that was requested
 * @param size          page size that was requested
 * @param totalElements number of elements in the whole result set
 * @param totalPages    number of pages needed to hold the whole result set
 * @param <T>           element type
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	/**
	 * Builds the response from a domain page.
	 *
	 * @param page the domain page
	 * @param <T>  element type
	 * @return the response body
	 */
	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.content(), page.page(), page.size(), page.totalElements(), page.totalPages());
	}

}
