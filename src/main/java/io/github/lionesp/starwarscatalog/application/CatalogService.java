package io.github.lionesp.starwarscatalog.application;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogPort;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogItemNotFoundException;
import io.github.lionesp.starwarscatalog.domain.model.CatalogItem;
import io.github.lionesp.starwarscatalog.domain.page.InMemoryPaginator;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;

import java.util.List;

/**
 * Use cases to list and fetch the items of one catalog resource.
 *
 * @param <T> entity type of the resource
 */
public class CatalogService<T extends CatalogItem> {

	private final CatalogPort<T> port;

	/**
	 * Creates the service.
	 *
	 * @param port source of the resource
	 */
	public CatalogService(CatalogPort<T> port) {
		this.port = port;
	}

	/**
	 * Lists items applying the optional filters and pagination.
	 *
	 * @param filter id and name filters
	 * @param query  the requested page
	 * @return the page of matches
	 */
	public Page<T> list(CatalogFilter filter, PageQuery query) {
		if (filter.idFilter().isPresent()) {
			return InMemoryPaginator.paginate(findByIdAndName(filter), query);
		}
		if (filter.nameFilter().isPresent()) {
			return InMemoryPaginator.paginate(port.findAllByName(filter.name()), query);
		}
		return port.findPage(query);
	}

	/**
	 * Fetches one item by id.
	 *
	 * @param rawId raw id text
	 * @return the item
	 * @throws io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException if the id is invalid
	 * @throws io.github.lionesp.starwarscatalog.domain.catalog.CatalogItemNotFoundException if it does not exist
	 */
	public T get(String rawId) {
		String id = CatalogFilter.validateId(rawId);
		return port.findById(id).orElseThrow(() -> new CatalogItemNotFoundException(id));
	}

	private List<T> findByIdAndName(CatalogFilter filter) {
		return port.findById(filter.id()).filter(filter::matchesName).stream().toList();
	}

}
