package io.github.lionesp.starwarscatalog.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class CaffeineCatalogCacheTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
		.withUserConfiguration(CacheConfig.class, CaffeineCatalogCache.class)
		.withPropertyValues("spring.cache.cache-names=swapi",
				"spring.cache.caffeine.spec=maximumSize=10,recordStats");

	@Test
	void E8_evictAll_removesEveryEntryOfTheSwapiCache() {
		runner.run(context -> {
			Cache swapiCache = context.getBean(CacheManager.class).getCache("swapi");
			swapiCache.put("people-1", "luke");
			swapiCache.put("films-1", "a-new-hope");

			context.getBean(CaffeineCatalogCache.class).evictAll();

			assertThat(swapiCache.get("people-1")).isNull();
			assertThat(swapiCache.get("films-1")).isNull();
		});
	}

	@Test
	void E8_cacheAcceptsNullValues() {
		runner.run(context -> {
			Cache swapiCache = context.getBean(CacheManager.class).getCache("swapi");

			swapiCache.put("people-9999", null);

			assertThat(swapiCache.get("people-9999")).isNotNull();
		});
	}

}
