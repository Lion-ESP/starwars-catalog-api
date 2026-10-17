package io.github.lionesp.starwarscatalog.adapter.out.swapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Properties of a SWAPI person; every value arrives as text.
 *
 * @param name      name
 * @param gender    gender
 * @param birthYear birth year
 * @param height    height
 * @param mass      mass
 * @param hairColor hair color
 * @param skinColor skin color
 * @param eyeColor  eye color
 * @param homeworld home planet URL
 * @param films     film URLs
 * @param starships starship URLs
 * @param vehicles  vehicle URLs
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwapiPersonProperties(String name, String gender, @JsonProperty("birth_year") String birthYear,
		String height, String mass, @JsonProperty("hair_color") String hairColor,
		@JsonProperty("skin_color") String skinColor, @JsonProperty("eye_color") String eyeColor,
		String homeworld, List<String> films, List<String> starships, List<String> vehicles) {

}
