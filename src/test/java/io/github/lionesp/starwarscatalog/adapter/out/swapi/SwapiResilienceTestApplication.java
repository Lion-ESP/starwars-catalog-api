package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import io.github.lionesp.starwarscatalog.adapter.out.cache.CaffeineCatalogCache;
import io.github.lionesp.starwarscatalog.config.CacheConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableConfigurationProperties(SwapiProperties.class)
@Import({ SwapiClientConfig.class, SwapiClient.class, SwapiResilience.class, CacheConfig.class,
		CaffeineCatalogCache.class })
class SwapiResilienceTestApplication {

}
