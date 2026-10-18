package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AdminControllerTest extends CatalogWebTestSupport {

	private static final String CACHE_URL = "/api/v1/admin/cache";

	@Test
	void userTokenIsForbidden() throws Exception {
		mockMvc.perform(delete(CACHE_URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(403));

		verifyNoInteractions(cacheAdminService);
	}

	@Test
	void adminTokenEvictsTheCache() throws Exception {
		mockMvc.perform(delete(CACHE_URL).header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isNoContent());

		verify(cacheAdminService).evictAll();
	}
}
