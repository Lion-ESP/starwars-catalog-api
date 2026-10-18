package io.github.lionesp.starwarscatalog.adapter.out.swapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Envelope of a SWAPI detail, where {@code result} is a single item.
 *
 * @param <P> type of the properties payload
 * @param result the item
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwapiItemResponse<P>(SwapiItem<P> result) {

}
