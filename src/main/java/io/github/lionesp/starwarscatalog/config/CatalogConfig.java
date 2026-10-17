package io.github.lionesp.starwarscatalog.config;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiCatalogAdapter;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiClient;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiResource;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiFilmProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiStarshipProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiVehicleProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.FilmMapper;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.PersonMapper;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.StarshipMapper;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper.VehicleMapper;
import io.github.lionesp.starwarscatalog.application.CacheAdminService;
import io.github.lionesp.starwarscatalog.application.CatalogService;
import io.github.lionesp.starwarscatalog.application.port.out.CatalogCache;
import io.github.lionesp.starwarscatalog.domain.model.Film;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import io.github.lionesp.starwarscatalog.domain.model.Starship;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root: wires the framework-free use cases to their adapters.
 */
@Configuration(proxyBeanMethods = false)
public class CatalogConfig {

	/**
	 * Provides the clock used to compute {@code Retry-After} values.
	 *
	 * @return the system UTC clock
	 */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	/**
	 * Builds the people use cases.
	 *
	 * @param swapiClient SWAPI client
	 * @return the people service
	 */
	@Bean
	public CatalogService<Person> peopleService(SwapiClient swapiClient) {
		return new CatalogService<>(new SwapiCatalogAdapter<>(SwapiResource.PEOPLE, swapiClient,
				new PersonMapper(), SwapiPersonProperties.class));
	}

	/**
	 * Builds the films use cases.
	 *
	 * @param swapiClient SWAPI client
	 * @return the films service
	 */
	@Bean
	public CatalogService<Film> filmsService(SwapiClient swapiClient) {
		return new CatalogService<>(new SwapiCatalogAdapter<>(SwapiResource.FILMS, swapiClient,
				new FilmMapper(), SwapiFilmProperties.class));
	}

	/**
	 * Builds the starships use cases.
	 *
	 * @param swapiClient SWAPI client
	 * @return the starships service
	 */
	@Bean
	public CatalogService<Starship> starshipsService(SwapiClient swapiClient) {
		return new CatalogService<>(new SwapiCatalogAdapter<>(SwapiResource.STARSHIPS, swapiClient,
				new StarshipMapper(), SwapiStarshipProperties.class));
	}

	/**
	 * Builds the vehicles use cases.
	 *
	 * @param swapiClient SWAPI client
	 * @return the vehicles service
	 */
	@Bean
	public CatalogService<Vehicle> vehiclesService(SwapiClient swapiClient) {
		return new CatalogService<>(new SwapiCatalogAdapter<>(SwapiResource.VEHICLES, swapiClient,
				new VehicleMapper(), SwapiVehicleProperties.class));
	}

	/**
	 * Builds the cache administration use cases.
	 *
	 * @param cache the catalog cache
	 * @return the cache admin service
	 */
	@Bean
	public CacheAdminService cacheAdminService(CatalogCache cache) {
		return new CacheAdminService(cache);
	}

}
