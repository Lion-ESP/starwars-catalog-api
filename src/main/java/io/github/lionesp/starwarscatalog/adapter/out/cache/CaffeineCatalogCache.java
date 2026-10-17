package io.github.lionesp.starwarscatalog.adapter.out.cache;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogCache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Caffeine-backed implementation of {@link CatalogCache}: clears the {@code swapi} cache.
 */
@Component
public class CaffeineCatalogCache implements CatalogCache {

	private static final String CACHE_NAME = "swapi";

	private final CacheManager cacheManager;

	/**
	 * Creates the adapter.
	 *
	 * @param cacheManager the Spring cache manager
	 */
	public CaffeineCatalogCache(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public void evictAll() {
		Cache cache = cacheManager.getCache(CACHE_NAME);
		if (cache != null) {
			cache.clear();
		}
	}

}
