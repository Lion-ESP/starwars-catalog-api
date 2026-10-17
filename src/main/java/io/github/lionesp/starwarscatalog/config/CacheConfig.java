package io.github.lionesp.starwarscatalog.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction. The {@code swapi} cache itself is declared through
 * {@code spring.cache.*}, so Boot builds the Caffeine manager and registers its metrics in Micrometer.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

}
