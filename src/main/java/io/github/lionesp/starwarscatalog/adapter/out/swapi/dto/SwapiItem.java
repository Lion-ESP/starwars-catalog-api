package io.github.lionesp.starwarscatalog.adapter.out.swapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A SWAPI resource wrapper holding its {@code uid} and its properties.
 *
 * @param <P> type of the properties payload
 * @param uid        SWAPI identifier
 * @param properties resource properties
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwapiItem<P>(String uid, P properties) {

}
