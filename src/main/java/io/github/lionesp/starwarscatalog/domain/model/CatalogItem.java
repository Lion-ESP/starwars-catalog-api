package io.github.lionesp.starwarscatalog.domain.model;

/**
 * Common contract of every entity exposed by the catalog.
 */
public sealed interface CatalogItem permits Person, Film, Starship, Vehicle {

	/**
	 * Returns the SWAPI {@code uid} of the entity.
	 *
	 * @return the identifier as text
	 */
	String id();

	/**
	 * Returns the display name used for name filtering; for films it is the title.
	 *
	 * @return the name of the entity, possibly {@code null} if SWAPI omitted it
	 */
	String name();

}
