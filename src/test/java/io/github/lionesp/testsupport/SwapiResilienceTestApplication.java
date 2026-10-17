package io.github.lionesp.testsupport;

import io.github.lionesp.starwarscatalog.adapter.out.cache.CaffeineCatalogCache;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiClient;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiClientConfig;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiResilience;
import io.github.lionesp.starwarscatalog.config.CacheConfig;
import java.time.Clock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Small context with the SWAPI client, resilience and cache. It lives outside the application package so the
 * component scan of the full application never picks it up.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableConfigurationProperties(SwapiProperties.class)
@Import({ SwapiClientConfig.class, SwapiClient.class, SwapiResilience.class, CacheConfig.class,
		CaffeineCatalogCache.class })
public class SwapiResilienceTestApplication {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
