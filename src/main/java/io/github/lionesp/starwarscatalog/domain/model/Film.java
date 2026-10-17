package io.github.lionesp.starwarscatalog.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A Star Wars film. Unknown values are {@code null}.
 *
 * @param id           SWAPI uid
 * @param title        film title
 * @param episodeId    episode number
 * @param director     director
 * @param producer     producer
 * @param releaseDate  release date
 * @param openingCrawl opening crawl text
 * @param characterIds ids of the characters
 * @param planetIds    ids of the planets
 * @param starshipIds  ids of the starships
 * @param vehicleIds   ids of the vehicles
 * @param speciesIds   ids of the species
 */
public record Film(String id, String title, Integer episodeId, String director, String producer,
		LocalDate releaseDate, String openingCrawl, List<String> characterIds, List<String> planetIds,
		List<String> starshipIds, List<String> vehicleIds, List<String> speciesIds) implements CatalogItem {

	/**
	 * Validates the record, copying the lists defensively ({@code null} becomes empty).
	 */
	public Film {
		characterIds = characterIds == null ? List.of() : List.copyOf(characterIds);
		planetIds = planetIds == null ? List.of() : List.copyOf(planetIds);
		starshipIds = starshipIds == null ? List.of() : List.copyOf(starshipIds);
		vehicleIds = vehicleIds == null ? List.of() : List.copyOf(vehicleIds);
		speciesIds = speciesIds == null ? List.of() : List.copyOf(speciesIds);
	}

	/**
	 * Returns the film title, which acts as its name.
	 *
	 * @return the title
	 */
	@Override
	public String name() {
		return title;
	}

}
