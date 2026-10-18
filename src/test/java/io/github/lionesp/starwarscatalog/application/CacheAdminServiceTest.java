package io.github.lionesp.starwarscatalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogCache;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CacheAdminServiceTest {

	@Test
	void evictAllDelegatesToCache() {
		AtomicInteger evictions = new AtomicInteger();
		CatalogCache countingCache = evictions::incrementAndGet;

		new CacheAdminService(countingCache).evictAll();

		assertThat(evictions.get()).isEqualTo(1);
	}

}
