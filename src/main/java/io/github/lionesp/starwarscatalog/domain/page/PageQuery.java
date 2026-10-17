package io.github.lionesp.starwarscatalog.domain.page;

import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;

/**
 * Pagination request: a 1-based page number and a page size.
 * <p>
 * {@code page} must be at least {@value #MIN_PAGE} with no upper bound, and {@code size}
 * must be between {@value #MIN_SIZE} and {@value #MAX_SIZE}.
 *
 * @param page 1-based page number
 * @param size number of elements per page
 */
public record PageQuery(int page, int size) {

	/** Smallest page number a client may request. */
	public static final int MIN_PAGE = 1;

	/** Smallest size a client may request. */
	public static final int MIN_SIZE = 1;

	/** Page used when the client sends none. */
	public static final int DEFAULT_PAGE = 1;

	/** Size used when the client sends none. */
	public static final int DEFAULT_SIZE = 10;

	/** Largest size a client may request. */
	public static final int MAX_SIZE = 100;

	/**
	 * Validates the ranges of both values.
	 *
	 * @throws InvalidParameterException if {@code page} or {@code size} is out of range
	 */
	public PageQuery {
		if (page < MIN_PAGE) {
			throw new InvalidParameterException("page", "page must be greater than or equal to " + MIN_PAGE);
		}
		if (size < MIN_SIZE || size > MAX_SIZE) {
			throw new InvalidParameterException("size", "size must be between " + MIN_SIZE + " and " + MAX_SIZE);
		}
	}

	/**
	 * Builds a query applying defaults to absent values.
	 *
	 * @param page 1-based page number, or {@code null} for {@value #DEFAULT_PAGE}
	 * @param size page size, or {@code null} for {@value #DEFAULT_SIZE}
	 * @return the validated query
	 * @throws InvalidParameterException if a provided value is out of range
	 */
	public static PageQuery of(Integer page, Integer size) {
		return new PageQuery(page == null ? DEFAULT_PAGE : page, size == null ? DEFAULT_SIZE : size);
	}

}
