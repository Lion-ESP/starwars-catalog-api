package io.github.lionesp.starwarscatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the Star Wars catalog API.
 * Exposes people, films, starships and vehicles backed by swapi.tech.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StarwarsCatalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(StarwarsCatalogApplication.class, args);
	}

}
