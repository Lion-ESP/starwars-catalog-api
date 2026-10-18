package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.security.TokenService;
import io.github.lionesp.testsupport.TestCredentials;
import io.github.lionesp.testsupport.TestProperties;
import io.github.lionesp.testsupport.TestTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = { TestCredentials.JWT_SECRET_PROPERTY,
		TestProperties.UNREACHABLE_SWAPI_BASE_URL_PROPERTY })
@AutoConfigureMockMvc
class ActuatorAndDocsTest {

	private static final String METRICS_PATH = "/actuator/metrics";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenService tokenService;

	private enum Role {

		USER, ADMIN;

		String bearer(TokenService tokenService) {
			return this == ADMIN ? TestTokens.adminBearer(tokenService) : TestTokens.userBearer(tokenService);
		}

	}

	@ParameterizedTest(name = "{0} is public")
	@ValueSource(strings = { "/actuator/health", "/swagger-ui/index.html", "/v3/api-docs",
			"/v3/api-docs/swagger-config" })
	void publicEndpointsAnswerWithoutToken(String path) throws Exception {
		mockMvc.perform(get(path)).andExpect(status().isOk());
	}

	@Test
	void swaggerUiShortcutRedirectsToTheUi() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string(HttpHeaders.LOCATION, "/swagger-ui/index.html"));
	}

	@Test
	void metricsWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get(METRICS_PATH)).andExpect(status().isUnauthorized());
	}

	@Test
	void metricsWithUserTokenIsForbidden() throws Exception {
		mockMvc.perform(get(METRICS_PATH).header(HttpHeaders.AUTHORIZATION, Role.USER.bearer(tokenService)))
			.andExpect(status().isForbidden());
	}

	@Test
	void metricsWithAdminTokenIsOk() throws Exception {
		mockMvc.perform(get(METRICS_PATH).header(HttpHeaders.AUTHORIZATION, Role.ADMIN.bearer(tokenService)))
			.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "{0} is not exposed to {1}")
	@CsvSource({ "/actuator/info,USER", "/actuator/env,USER", "/actuator/beans,USER", "/actuator/info,ADMIN",
			"/actuator/env,ADMIN", "/actuator/beans,ADMIN" })
	void notExposedEndpointsAreNotFound(String path, Role role) throws Exception {
		mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, role.bearer(tokenService)))
			.andExpect(status().isNotFound());
	}

	@Test
	void apiDocsDescribeCatalogAndSecurity() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/people")))
			.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/films")))
			.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/starships")))
			.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/vehicles")))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

}
