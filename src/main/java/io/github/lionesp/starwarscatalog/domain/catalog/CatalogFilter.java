package io.github.lionesp.starwarscatalog.domain.catalog;

import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import io.github.lionesp.starwarscatalog.domain.model.CatalogItem;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Optional;

/**
 * Optional filters for a catalog query: an exact id and a case-insensitive partial name.
 *
 * @param id   normalized positive id, or {@code null} when absent
 * @param name trimmed non-blank name fragment, or {@code null} when absent
 */
public record CatalogFilter(String id, String name) {

	/** Longest name fragment a client may filter by, after trimming. */
	public static final int MAX_NAME_LENGTH = 100;

	private static final String ID_PARAMETER = "id";

	private static final String NAME_PARAMETER = "name";

	/**
	 * Builds a filter from raw client input.
	 *
	 * @param rawId   raw id text; {@code null} or blank means absent
	 * @param rawName raw name text; {@code null} or blank means absent
	 * @return the filter
	 * @throws InvalidParameterException if {@code rawId} is present but not a positive integer, or if the
	 *                                   trimmed {@code rawName} is longer than {@value #MAX_NAME_LENGTH}
	 */
	public static CatalogFilter of(String rawId, String rawName) {
		String id = rawId == null || rawId.isBlank() ? null : validateId(rawId);
		String name = rawName == null || rawName.isBlank() ? null : validateName(rawName);
		return new CatalogFilter(id, name);
	}

	private static String validateName(String rawName) {
		String trimmed = rawName.trim();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new InvalidParameterException(NAME_PARAMETER,
					"name must be at most " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	/**
	 * Validates an id with the same rule used for the {@code id} filter, for example a path id.
	 *
	 * @param rawId raw id text
	 * @return the id without surrounding spaces or leading zeros
	 * @throws InvalidParameterException if the text is not made only of digits or is not greater than zero
	 */
	public static String validateId(String rawId) {
		String trimmed = rawId == null ? "" : rawId.trim();
		if (!trimmed.matches("[0-9]+")) {
			throw new InvalidParameterException(ID_PARAMETER, "id must be a positive integer");
		}
		BigInteger value = new BigInteger(trimmed);
		if (value.signum() <= 0) {
			throw new InvalidParameterException(ID_PARAMETER, "id must be greater than zero");
		}
		return value.toString();
	}

	/**
	 * Returns the id filter.
	 *
	 * @return the id, if present
	 */
	public Optional<String> idFilter() {
		return Optional.ofNullable(id);
	}

	/**
	 * Returns the name filter.
	 *
	 * @return the name fragment, if present
	 */
	public Optional<String> nameFilter() {
		return Optional.ofNullable(name);
	}

	/**
	 * Tells whether an item satisfies the name filter: contains, ignoring case.
	 * An absent filter matches everything.
	 *
	 * @param item the item to test
	 * @return {@code true} if the item matches
	 */
	public boolean matchesName(CatalogItem item) {
		if (name == null) {
			return true;
		}
		String itemName = item.name();
		return itemName != null
				&& itemName.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT));
	}

}
