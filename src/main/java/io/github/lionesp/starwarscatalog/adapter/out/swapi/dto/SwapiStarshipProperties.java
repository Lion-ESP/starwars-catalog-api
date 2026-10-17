package io.github.lionesp.starwarscatalog.adapter.out.swapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Properties of a SWAPI starship; every value arrives as text.
 *
 * @param name                 name
 * @param model                model
 * @param manufacturer         manufacturer
 * @param starshipClass        class
 * @param costInCredits        cost
 * @param length               length
 * @param crew                 crew
 * @param passengers           passengers
 * @param cargoCapacity        cargo capacity
 * @param maxAtmospheringSpeed max speed in atmosphere
 * @param hyperdriveRating     hyperdrive rating
 * @param mglt                 megalights per hour
 * @param consumables          consumables
 * @param pilots               pilot URLs
 * @param films                film URLs
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwapiStarshipProperties(String name, String model, String manufacturer,
		@JsonProperty("starship_class") String starshipClass,
		@JsonProperty("cost_in_credits") String costInCredits, String length, String crew, String passengers,
		@JsonProperty("cargo_capacity") String cargoCapacity,
		@JsonProperty("max_atmosphering_speed") String maxAtmospheringSpeed,
		@JsonProperty("hyperdrive_rating") String hyperdriveRating, @JsonProperty("MGLT") String mglt,
		String consumables, List<String> pilots, List<String> films) {

}
