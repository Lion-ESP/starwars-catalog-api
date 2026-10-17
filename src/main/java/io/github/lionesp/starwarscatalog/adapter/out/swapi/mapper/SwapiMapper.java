package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.domain.model.CatalogItem;

/**
 * Maps a SWAPI item to a domain entity.
 *
 * @param <P> type of the SWAPI properties
 * @param <T> domain entity type
 */
public interface SwapiMapper<P, T extends CatalogItem> {

	/**
	 * Converts a SWAPI item into the domain entity; the id comes from the {@code uid}.
	 *
	 * @param item SWAPI item
	 * @return the domain entity
	 */
	T toDomain(SwapiItem<P> item);

}
