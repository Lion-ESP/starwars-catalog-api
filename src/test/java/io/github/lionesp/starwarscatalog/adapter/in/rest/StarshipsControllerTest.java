package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.model.Starship;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class StarshipsControllerTest extends CatalogWebTestSupport {

	private static final String URL = "/api/v1/starships";

	private static Starship sample() {
		return new Starship("9", "Death Star", "DS-1", "Imperial", "Space station", 1000000000000L, 120000.0, 342953L, 843342L, 1000000000L, 10L, 4.0, 3L, "3 years", List.of(), List.of());
	}

	@Test
	@DisplayName("E15: el listado de starships pagina y responde con la forma de PageResponse")
	void listDelegatesToTheService() throws Exception {
		when(starshipsService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(sample()), 1, 10, 1, 1));

		mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value("9"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		verify(starshipsService).list(CatalogFilter.of(null, null), new PageQuery(1, 10));
	}

	@Test
	@DisplayName("E16 y E17: el filtro name llega al servicio de starships")
	void nameFilterReachesTheService() throws Exception {
		when(starshipsService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(sample()), 1, 10, 1, 1));

		mockMvc.perform(get(URL).param("name", "hope").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk());

		verify(starshipsService).list(CatalogFilter.of(null, "hope"), new PageQuery(1, 10));
	}

	@Test
	@DisplayName("E24: el detalle de starships delega en get(id)")
	void detailDelegatesToGet() throws Exception {
		when(starshipsService.get("9")).thenReturn(sample());

		mockMvc.perform(get(URL + "/9").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("9"))
				.andExpect(jsonPath("$.name").value("Death Star"));
	}
}
