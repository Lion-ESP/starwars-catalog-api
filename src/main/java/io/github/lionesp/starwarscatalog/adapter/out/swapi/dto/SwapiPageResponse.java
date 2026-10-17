package io.github.lionesp.starwarscatalog.adapter.out.swapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Paginated listing envelope of SWAPI ({@code results}).
 *
 * @param <P> type of the properties payload
 * @param totalRecords total number of records
 * @param totalPages   total number of pages
 * @param results      items of the page
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SwapiPageResponse<P>(@JsonProperty("total_records") Integer totalRecords,
		@JsonProperty("total_pages") Integer totalPages, List<SwapiItem<P>> results) {

}
