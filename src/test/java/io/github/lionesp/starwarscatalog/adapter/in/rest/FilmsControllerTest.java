package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.model.Film;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class FilmsControllerTest extends CatalogWebTestSupport {

	private static final String URL = "/api/v1/films";

	private static Film sample() {
		return new Film("1", "A New Hope", 4, "George Lucas", "Gary Kurtz", java.time.LocalDate.of(1977, 5, 25), "crawl", List.of(), List.of(), List.of(), List.of(), List.of());
	}

	@Test
	void listDelegatesToTheService() throws Exception {
		when(filmsService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(sample()), 1, 10, 1, 1));

		mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value("1"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		verify(filmsService).list(CatalogFilter.of(null, null), new PageQuery(1, 10));
	}

	@Test
	void nameFilterReachesTheService() throws Exception {
		when(filmsService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(sample()), 1, 10, 1, 1));

		mockMvc.perform(get(URL).param("name", "hope").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk());

		verify(filmsService).list(CatalogFilter.of(null, "hope"), new PageQuery(1, 10));
	}

	@Test
	void detailDelegatesToGet() throws Exception {
		when(filmsService.get("1")).thenReturn(sample());

		mockMvc.perform(get(URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("1"))
				.andExpect(jsonPath("$.title").value("A New Hope"));
	}
}
