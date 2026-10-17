package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.security.TokenService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"app.security.jwt.secret=" + ActuatorAndDocsTest.SECRET,
		"swapi.base-url=http://invalid.invalid:1/api"
})
@AutoConfigureMockMvc
class ActuatorAndDocsTest {

	static final String SECRET = "full-app-test-secret-with-more-than-32-bytes";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private TokenService tokenService;

	private String userToken() {
		return "Bearer " + tokenService.issue("user", List.of("USER"));
	}

	private String adminToken() {
		return "Bearer " + tokenService.issue("admin", List.of("USER", "ADMIN"));
	}

	@ParameterizedTest(name = "E9: {0} es público")
	@ValueSource(strings = {"/actuator/health", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config"})
	void publicEndpointsAnswerWithoutToken(String path) throws Exception {
		mockMvc.perform(get(path)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("E43: metrics sin token es 401")
	void metricsWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("E43: metrics con token USER es 403")
	void metricsWithUserTokenIsForbidden() throws Exception {
		mockMvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("E43: metrics con token ADMIN es 200")
	void metricsWithAdminTokenIsOk() throws Exception {
		mockMvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "E44: {0} con token USER no está expuesto")
	@ValueSource(strings = {"/actuator/info", "/actuator/env", "/actuator/beans"})
	void notExposedEndpointsAreNotFound(String path) throws Exception {
		mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isNotFound());
	}

	@ParameterizedTest(name = "E44: {0} con token ADMIN no está expuesto")
	@ValueSource(strings = {"/actuator/info", "/actuator/env", "/actuator/beans"})
	void notExposedEndpointsAreNotFoundForAdmin(String path) throws Exception {
		mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, adminToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("N5: /v3/api-docs describe los cuatro recursos y el esquema bearerAuth")
	void apiDocsDescribeCatalogAndSecurity() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/people")))
				.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/films")))
				.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/starships")))
				.andExpect(jsonPath("$.paths").value(hasKey("/api/v1/vehicles")))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	@DisplayName("El contexto completo no incluye ninguna clase de test")
	void fullContextHasNoBeanDefinedInTestClasses() {
		List<String> beansFromTestClasses = Arrays.stream(applicationContext.getBeanDefinitionNames())
				.filter(name -> {
					Class<?> type = applicationContext.getType(name);
					return type != null && type.getProtectionDomain().getCodeSource() != null
							&& type.getProtectionDomain().getCodeSource().getLocation().getPath().contains("test-classes");
				})
				.toList();

		assertThat(beansFromTestClasses).isEmpty();
	}
}
