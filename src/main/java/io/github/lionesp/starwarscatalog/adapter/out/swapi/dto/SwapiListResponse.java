package io.github.lionesp.starwarscatalog.adapter.out.swapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Envelope of SWAPI searches and films, where {@code result} is a list.
 *
 * @param <P> type of the properties payload
 * @param result matching items
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwapiListResponse<P>(List<SwapiItem<P>> result) {

}
