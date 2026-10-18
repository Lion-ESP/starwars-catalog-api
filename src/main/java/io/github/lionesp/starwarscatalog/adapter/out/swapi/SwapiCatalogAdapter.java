package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPageResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.SwapiMapper;
import io.github.lionesp.starwarscatalog.application.port.out.CatalogPort;
import io.github.lionesp.starwarscatalog.domain.model.CatalogItem;
import io.github.lionesp.starwarscatalog.domain.page.InMemoryPaginator;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalog port backed by SWAPI for one resource: picks the pagination strategy and maps the provider DTOs to
 * the domain.
 *
 * @param <P> SWAPI properties type
 * @param <T> domain type
 */
public class SwapiCatalogAdapter<P, T extends CatalogItem> implements CatalogPort<T> {

	private final SwapiResource resource;

	private final SwapiClient client;

	private final SwapiMapper<P, T> mapper;

	private final Class<P> propertiesType;

	/**
	 * Creates the adapter.
	 *
	 * @param resource       SWAPI resource served by this adapter
	 * @param client         SWAPI client
	 * @param mapper         maps provider properties to the domain type
	 * @param propertiesType type of the provider properties
	 */
	public SwapiCatalogAdapter(SwapiResource resource, SwapiClient client, SwapiMapper<P, T> mapper,
			Class<P> propertiesType) {
		this.resource = resource;
		this.client = client;
		this.mapper = mapper;
		this.propertiesType = propertiesType;
	}

	@Override
	public Page<T> findPage(PageQuery query) {
		if (!resource.paginated()) {
			return InMemoryPaginator.paginate(toDomain(client.fetchAll(resource, propertiesType)), query);
		}
		SwapiPageResponse<P> response = client.fetchPage(resource, propertiesType, query.page(), query.size());
		int totalRecords = response.totalRecords();
		int totalPages = response.totalPages();
		if (query.page() > totalPages) {
			return Page.empty(query, totalRecords);
		}
		return Page.of(toDomain(response.results()), query, totalRecords);
	}

	@Override
	public List<T> findAllByName(String name) {
		return toDomain(client.search(resource, propertiesType, name.toLowerCase(Locale.ROOT)));
	}

	@Override
	public Optional<T> findById(String id) {
		return client.fetchById(resource, propertiesType, id).map(mapper::toDomain);
	}

	private List<T> toDomain(List<SwapiItem<P>> items) {
		return items.stream().map(mapper::toDomain).toList();
	}

}
