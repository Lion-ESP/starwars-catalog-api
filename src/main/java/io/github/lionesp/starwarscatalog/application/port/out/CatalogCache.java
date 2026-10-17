package io.github.lionesp.starwarscatalog.application.port.out;

/**
 * Outbound port to the cache that holds catalog data.
 */
public interface CatalogCache {

	/**
	 * Removes every cached entry.
	 */
	void evictAll();

}
