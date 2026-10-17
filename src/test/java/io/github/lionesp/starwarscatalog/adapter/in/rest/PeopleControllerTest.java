package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogItemNotFoundException;
import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PeopleControllerTest extends CatalogWebTestSupport {

	private static final String PEOPLE_URL = "/api/v1/people";

	private static Person luke() {
		return new Person("1", "Luke Skywalker", "male", "19BBY", 172L, 77.0, "blond", "fair", "blue", "1",
				List.of("1", "2"), List.of("12"), List.of("14"));
	}

	@Test
	@DisplayName("E10: page y size llegan al servicio y la respuesta tiene la forma exacta de PageResponse")
	void listDelegatesAndRespondsWithPageShape() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(luke()), 2, 10, 82, 9));

		mockMvc.perform(get(PEOPLE_URL).param("page", "2").param("size", "10")
						.header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value("1"))
				.andExpect(jsonPath("$.content[0].name").value("Luke Skywalker"))
				.andExpect(jsonPath("$.content[0].height").value(172))
				.andExpect(jsonPath("$.content[0].filmIds.length()").value(2))
				.andExpect(jsonPath("$.page").value(2))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(82))
				.andExpect(jsonPath("$.totalPages").value(9))
				.andExpect(jsonPath("$.length()").value(5));

		ArgumentCaptor<PageQuery> query = ArgumentCaptor.forClass(PageQuery.class);
		verify(peopleService).list(eq(CatalogFilter.of(null, null)), query.capture());
		org.assertj.core.api.Assertions.assertThat(query.getValue()).isEqualTo(new PageQuery(2, 10));
	}

	@Test
	@DisplayName("E6 y E11: token USER sin page ni size usa los defaults 1 y 10")
	void userTokenWithoutPagingUsesDefaults() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(), 1, 10, 0, 0));

		mockMvc.perform(get(PEOPLE_URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk());

		verify(peopleService).list(CatalogFilter.of(null, null), new PageQuery(1, 10));
	}

	@Test
	@DisplayName("E16 y E19: name e id llegan normalizados al filtro")
	void nameAndIdFiltersReachTheService() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(), 1, 1, 0, 0));

		mockMvc.perform(get(PEOPLE_URL).param("name", "sky").param("id", "1").param("size", "1")
						.header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk());

		verify(peopleService).list(CatalogFilter.of("1", "sky"), new PageQuery(1, 1));
	}

	@Test
	@DisplayName("E19: name en blanco se ignora")
	void blankNameIsIgnored() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(), 1, 10, 0, 0));

		mockMvc.perform(get(PEOPLE_URL).param("name", " ").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk());

		verify(peopleService).list(CatalogFilter.of(null, null), new PageQuery(1, 10));
	}

	@Test
	@DisplayName("E12, E45, E46 y E47: una página vacía con totales reales se responde 200")
	void emptyPageWithRealTotalsIsOk() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(), 2147483647, 100, 82, 1));

		mockMvc.perform(get(PEOPLE_URL).param("page", "2147483647").param("size", "100")
						.header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0))
				.andExpect(jsonPath("$.totalElements").value(82))
				.andExpect(jsonPath("$.totalPages").value(1));
	}

	@ParameterizedTest(name = "E13: {0}={1} responde 400 que nombra {0}")
	@CsvSource({"page,0", "page,-1", "page,abc", "size,0", "size,101", "size,abc", "size,1.5"})
	void invalidPagingNamesTheParameter(String parameter, String value) throws Exception {
		mockMvc.perform(get(PEOPLE_URL).param(parameter, value).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value(containsString(parameter)));
	}

	@ParameterizedTest(name = "E23: id={0} en el query responde 400 que nombra id")
	@ValueSource(strings = {"abc", "0", "-3", "1.5"})
	void invalidIdInQueryNamesTheParameter(String value) throws Exception {
		mockMvc.perform(get(PEOPLE_URL).param("id", value).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.parameter").value("id"))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	@DisplayName("E23: id inválido en la ruta responde 400 que nombra id")
	void invalidIdInPathNamesTheParameter() throws Exception {
		when(peopleService.get("abc")).thenThrow(new InvalidParameterException("id", "id must be a positive integer"));

		mockMvc.perform(get(PEOPLE_URL + "/abc").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.parameter").value("id"));
	}

	@Test
	@DisplayName("E24: el detalle delega en get(id) y responde la entidad")
	void detailDelegatesToGet() throws Exception {
		when(peopleService.get("1")).thenReturn(luke());

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("1"))
				.andExpect(jsonPath("$.name").value("Luke Skywalker"))
				.andExpect(jsonPath("$.mass").value(77.0));
	}

	@Test
	@DisplayName("E25: un id inexistente responde 404 Problem Details")
	void missingIdIsNotFound() throws Exception {
		when(peopleService.get("9999")).thenThrow(new CatalogItemNotFoundException("9999"));

		mockMvc.perform(get(PEOPLE_URL + "/9999").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));
	}
}
