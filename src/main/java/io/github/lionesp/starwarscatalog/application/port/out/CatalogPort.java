package io.github.lionesp.starwarscatalog.application.port.out;

import io.github.lionesp.starwarscatalog.domain.model.CatalogItem;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port to the source of a catalog resource.
 *
 * @param <T> entity type of the resource
 */
public interface CatalogPort<T extends CatalogItem> {

	/**
	 * Fetches one page of the whole resource, without filters.
	 *
	 * @param query the requested page
	 * @return the page
	 */
	Page<T> findPage(PageQuery query);

	/**
	 * Fetches every item whose name contains the fragment, ignoring case.
	 *
	 * @param name name fragment
	 * @return all matches, empty if none
	 */
	List<T> findAllByName(String name);

	/**
	 * Fetches one item by id.
	 *
	 * @param id normalized id
	 * @return the item, or empty if it does not exist
	 */
	Optional<T> findById(String id);

}
