package io.github.lionesp.starwarscatalog.domain.model;

import java.util.List;

/**
 * A person (character) of the Star Wars universe. Unknown numeric values are {@code null}.
 *
 * @param id            SWAPI uid
 * @param name          full name
 * @param gender        gender as reported by SWAPI
 * @param birthYear     birth year as reported by SWAPI
 * @param height        height in centimeters
 * @param mass          mass in kilograms
 * @param hairColor     hair color
 * @param skinColor     skin color
 * @param eyeColor      eye color
 * @param homeworldId   id of the home planet
 * @param filmIds       ids of the films the person appears in
 * @param starshipIds   ids of the starships piloted
 * @param vehicleIds    ids of the vehicles piloted
 */
public record Person(String id, String name, String gender, String birthYear, Long height, Double mass,
		String hairColor, String skinColor, String eyeColor, String homeworldId, List<String> filmIds,
		List<String> starshipIds, List<String> vehicleIds) implements CatalogItem {

	/**
	 * Validates the record, copying the lists defensively ({@code null} becomes empty).
	 */
	public Person {
		filmIds = filmIds == null ? List.of() : List.copyOf(filmIds);
		starshipIds = starshipIds == null ? List.of() : List.copyOf(starshipIds);
		vehicleIds = vehicleIds == null ? List.of() : List.copyOf(vehicleIds);
	}

}
