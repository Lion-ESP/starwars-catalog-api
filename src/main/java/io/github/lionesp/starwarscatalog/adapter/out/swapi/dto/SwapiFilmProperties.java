package io.github.lionesp.starwarscatalog.adapter.out.swapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Properties of a SWAPI film; {@code episode_id} is numeric, the rest is text.
 *
 * @param title        title
 * @param episodeId    episode number
 * @param director     director
 * @param producer     producer
 * @param releaseDate  release date as text
 * @param openingCrawl opening crawl
 * @param characters   character URLs
 * @param planets      planet URLs
 * @param starships    starship URLs
 * @param vehicles     vehicle URLs
 * @param species      species URLs
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwapiFilmProperties(String title, @JsonProperty("episode_id") Integer episodeId, String director,
		String producer, @JsonProperty("release_date") String releaseDate,
		@JsonProperty("opening_crawl") String openingCrawl, List<String> characters, List<String> planets,
		List<String> starships, List<String> vehicles, List<String> species) {

}
