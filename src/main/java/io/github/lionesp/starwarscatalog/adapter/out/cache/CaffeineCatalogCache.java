package io.github.lionesp.starwarscatalog.adapter.out.cache;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiClient;
import io.github.lionesp.starwarscatalog.application.port.out.CatalogCache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Spring-cache implementation of {@link CatalogCache}: clears the SWAPI cache.
 */
@Component
public class CaffeineCatalogCache implements CatalogCache {

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
		Cache cache = cacheManager.getCache(SwapiClient.CACHE_NAME);
		if (cache == null) {
			throw new IllegalStateException("Cache '" + SwapiClient.CACHE_NAME + "' is not configured");
		}
		cache.clear();
	}

}
