package io.github.lionesp.starwarscatalog.domain.catalog;

/**
 * Signals that no catalog item exists for the requested id.
 */
public class CatalogItemNotFoundException extends RuntimeException {

	/** Id that was looked up. */
	private final String id;

	/**
	 * Creates the exception for a missing item.
	 *
	 * @param id the id that was looked up
	 */
	public CatalogItemNotFoundException(String id) {
		super("No catalog item found with id " + id);
		this.id = id;
	}

	/**
	 * Returns the id that was looked up.
	 *
	 * @return the missing id
	 */
	public String id() {
		return id;
	}

}
