package io.github.lionesp.starwarscatalog.application;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CacheAdminServiceTest {

	@Test
	@DisplayName("E8 evictAll delegates to the cache")
	void evictAllDelegatesToCache() {
		AtomicInteger evictions = new AtomicInteger();
		CatalogCache countingCache = evictions::incrementAndGet;

		new CacheAdminService(countingCache).evictAll();

		assertThat(evictions.get()).isEqualTo(1);
	}

}
