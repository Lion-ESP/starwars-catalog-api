package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class VehiclesControllerTest extends CatalogWebTestSupport {

	private static final String URL = "/api/v1/vehicles";

	private static Vehicle sample() {
		return new Vehicle("4", "Sand Crawler", "Digger", "Corellia", "wheeled", 150000L, 36.8, 46L, 30L, 50000L, 30L, "2 months", List.of(), List.of());
	}

	@Test
	void listDelegatesToTheService() throws Exception {
		when(vehiclesService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(sample()), 1, 10, 1, 1));

		mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value("4"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		verify(vehiclesService).list(CatalogFilter.of(null, null), new PageQuery(1, 10));
	}

	@Test
	void nameFilterReachesTheService() throws Exception {
		when(vehiclesService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(sample()), 1, 10, 1, 1));

		mockMvc.perform(get(URL).param("name", "hope").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk());

		verify(vehiclesService).list(CatalogFilter.of(null, "hope"), new PageQuery(1, 10));
	}

	@Test
	void detailDelegatesToGet() throws Exception {
		when(vehiclesService.get("4")).thenReturn(sample());

		mockMvc.perform(get(URL + "/4").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("4"))
				.andExpect(jsonPath("$.name").value("Sand Crawler"));
	}
}
