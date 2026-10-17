package io.github.lionesp.starwarscatalog.application;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogCache;

/**
 * Administrative use cases over the catalog cache.
 */
public class CacheAdminService {

	private final CatalogCache cache;

	/**
	 * Creates the service.
	 *
	 * @param cache the cache to administer
	 */
	public CacheAdminService(CatalogCache cache) {
		this.cache = cache;
	}

	/**
	 * Evicts every cached entry.
	 */
	public void evictAll() {
		cache.evictAll();
	}

}
