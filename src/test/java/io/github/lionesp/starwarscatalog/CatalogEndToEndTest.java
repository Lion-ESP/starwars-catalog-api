package io.github.lionesp.starwarscatalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.lionesp.starwarscatalog.adapter.in.rest.CorrelationIdFilter;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiClient;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiFixtures;
import io.github.lionesp.starwarscatalog.security.JwtProperties;
import io.github.lionesp.starwarscatalog.security.TokenService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
		"app.security.jwt.secret=" + CatalogEndToEndTest.SECRET,
		"resilience4j.retry.instances.swapi.wait-duration=10ms"
})
@AutoConfigureMockMvc
class CatalogEndToEndTest {

	static final String SECRET = "end-to-end-test-secret-with-more-than-32-bytes";

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@DynamicPropertySource
	static void swapiBaseUrl(DynamicPropertyRegistry registry) {
		registry.add("swapi.base-url", wireMock::baseUrl);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private JwtProperties jwtProperties;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@BeforeEach
	void resetState() {
		wireMock.resetAll();
		cacheManager.getCache(SwapiClient.CACHE_NAME).clear();
		circuitBreakerRegistry.circuitBreaker("swapi").reset();
	}

	private String login(String username, String password) throws Exception {
		String responseBody = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		JsonNode tokenResponse = jsonMapper.readTree(responseBody);
		return "Bearer " + tokenResponse.get("accessToken").asString();
	}

	private ResultActions getAs(String bearer, String path) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.get(path).header(HttpHeaders.AUTHORIZATION, bearer));
	}

	private void stubSwapi(String urlWithQuery, int status, String body) {
		wireMock.stubFor(get(urlEqualTo(urlWithQuery)).willReturn(aResponse().withStatus(status)
			.withHeader("Content-Type", "application/json")
			.withBody(body)));
	}

	private void stubPeoplePage() {
		String body = SwapiFixtures.page(SwapiFixtures.firstItem("people-page"), 10, 82, 9);
		stubSwapi("/people?page=2&limit=10&expanded=true", 200, body);
	}

	@Test
	@DisplayName("E1 + E10: login real y listado de people paginado llama a SWAPI con page, limit y expanded")
	void loginThenListPeopleCallsSwapiWithExpandedPagination() throws Exception {
		stubPeoplePage();
		String userBearer = login("user", "user-password");

		getAs(userBearer, "/api/v1/people?page=2&size=10")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(10))
			.andExpect(jsonPath("$.page").value(2))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.totalElements").value(82))
			.andExpect(jsonPath("$.totalPages").value(9));

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people?page=2&limit=10&expanded=true")));
	}

	@Test
	@DisplayName("E1: el login devuelve accessToken, tokenType Bearer y expiresIn configurado")
	void loginReturnsBearerToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"user\",\"password\":\"user-password\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(3600))
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	@DisplayName("E17: films?name=hope llama a SWAPI con title y nunca con name")
	void filmsSearchCallsSwapiWithTitle() throws Exception {
		String body = SwapiFixtures.list(SwapiFixtures.firstItem("films"), 1, "A New Hope ");
		stubSwapi("/films?title=hope", 200, body);
		String userBearer = login("user", "user-password");

		getAs(userBearer, "/api/v1/films?name=hope").andExpect(status().isOk());

		wireMock.verify(1, getRequestedFor(urlEqualTo("/films?title=hope")));
		wireMock.verify(0, getRequestedFor(urlEqualTo("/films?name=hope")));
	}

	@Test
	@DisplayName("E25 + E49: id inexistente es 404 Problem Details y repetirlo no vuelve a llamar a SWAPI")
	void unknownIdIsNotFoundAndCachedAcrossRequests() throws Exception {
		stubSwapi("/people/9999", 404, "{\"message\":\"not found\"}");
		String userBearer = login("user", "user-password");

		getAs(userBearer, "/api/v1/people/9999")
			.andExpect(status().isNotFound())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"));
		getAs(userBearer, "/api/v1/people/9999").andExpect(status().isNotFound());

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/9999")));
	}

	@Test
	@DisplayName("E33: SWAPI 500 persistente es 502 Problem Details tras 3 llamadas")
	void persistentServerErrorIsBadGatewayAfterThreeCalls() throws Exception {
		stubSwapi("/people/1", 500, "{}");
		String userBearer = login("user", "user-password");

		getAs(userBearer, "/api/v1/people/1")
			.andExpect(status().isBadGateway())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/problem+json"));

		wireMock.verify(3, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	@DisplayName("E37: SWAPI 429 con x-ratelimit-reset es 503 con Retry-After")
	void rateLimitedSwapiIsServiceUnavailableWithRetryAfter() throws Exception {
		long resetEpochSeconds = Instant.now().plusSeconds(20).getEpochSecond();
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(429)
			.withHeader("x-ratelimit-reset", String.valueOf(resetEpochSeconds))));
		String userBearer = login("user", "user-password");

		String retryAfter = getAs(userBearer, "/api/v1/people/1")
			.andExpect(status().isServiceUnavailable())
			.andReturn().getResponse().getHeader(HttpHeaders.RETRY_AFTER);

		assertThat(Long.parseLong(retryAfter)).isBetween(1L, 21L);
	}

	@Test
	@DisplayName("E8: DELETE /admin/cache es 204 y la misma consulta vuelve a llamar a SWAPI")
	void adminEvictionMakesTheSameQueryCallSwapiAgain() throws Exception {
		stubPeoplePage();
		String adminBearer = login("admin", "admin-password");
		getAs(adminBearer, "/api/v1/people?page=2&size=10").andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/admin/cache").header(HttpHeaders.AUTHORIZATION, adminBearer))
			.andExpect(status().isNoContent());
		getAs(adminBearer, "/api/v1/people?page=2&size=10").andExpect(status().isOk());

		wireMock.verify(2, getRequestedFor(urlEqualTo("/people?page=2&limit=10&expanded=true")));
	}

	@Test
	@DisplayName("E39: un 401 devuelve X-Request-Id generado")
	void unauthorizedResponseCarriesGeneratedRequestId() throws Exception {
		String requestId = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/people"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
			.andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);

		assertThat(requestId).isNotBlank();
	}

	@Test
	@DisplayName("E39: un 401 devuelve el mismo X-Request-Id válido entrante")
	void unauthorizedResponseEchoesIncomingRequestId() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/people").header(CorrelationIdFilter.HEADER_NAME, "client-req-42"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "client-req-42"));
	}

	@Test
	@DisplayName("E42: login con un bearer vencido o basura responde 200")
	void loginIgnoresExpiredOrGarbageBearer() throws Exception {
		Clock pastClock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
		String expiredToken = new TokenService(jwtProperties, pastClock).issue("user", List.of("USER"));
		String loginBody = "{\"username\":\"user\",\"password\":\"user-password\"}";

		for (String bearer : List.of("Bearer " + expiredToken, "Bearer basura.que.no.es.jwt")) {
			mockMvc.perform(post("/api/v1/auth/login")
					.header(HttpHeaders.AUTHORIZATION, bearer)
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginBody))
				.andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("E43: con ADMIN la métrica de caché y las de Resilience4j existen (E9 y E44 están en ActuatorAndDocsTest)")
	void adminSeesCacheAndResilienceMetrics() throws Exception {
		stubPeoplePage();
		String adminBearer = login("admin", "admin-password");
		getAs(adminBearer, "/api/v1/people?page=2&size=10").andExpect(status().isOk());

		getAs(adminBearer, "/actuator/metrics/cache.gets?tag=cache:swapi")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.measurements[0].value").value(greaterThanOrEqualTo(1.0)));
		getAs(adminBearer, "/actuator/metrics")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.names").value(hasItem("resilience4j.circuitbreaker.state")));
	}
}
