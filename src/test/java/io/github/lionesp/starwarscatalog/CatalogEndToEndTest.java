package io.github.lionesp.starwarscatalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.adapter.in.rest.CorrelationIdFilter;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiFixtures;
import io.github.lionesp.starwarscatalog.security.JwtProperties;
import io.github.lionesp.testsupport.SwapiWireMockSupport;
import io.github.lionesp.testsupport.TestCredentials;
import io.github.lionesp.testsupport.TestTokens;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = TestCredentials.JWT_SECRET_PROPERTY)
@AutoConfigureMockMvc
class CatalogEndToEndTest extends SwapiWireMockSupport {

	private static final String LOGIN_PATH = "/api/v1/auth/login";

	private static final String PEOPLE_PATH = "/api/v1/people";

	private static final String PEOPLE_PAGE_QUERY = "?page=2&size=10";

	private static final String SWAPI_PEOPLE_PAGE_URL = "/people?page=2&limit=10&expanded=true";

	private static final String CACHE_GETS_METRIC = "cache.gets?tag=cache:swapi";

	private static final String PEOPLE_REQUESTS_METRIC = "http.server.requests?tag=uri:" + PEOPLE_PATH;

	private static final String COUNT_STATISTIC = "COUNT";

	private static final long RATE_LIMIT_RESET_SECONDS = 20;

	private static final long RETRY_AFTER_LOWER_BOUND_SECONDS = 1;

	private static final long RETRY_AFTER_UPPER_BOUND_SECONDS = RATE_LIMIT_RESET_SECONDS + 1;

	private static final String REQUEST_ID_SENT_BY_CLIENT = "client-req-42";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProperties jwtProperties;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private String login(String username, String password) throws Exception {
		String responseBody = mockMvc.perform(post(LOGIN_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(TestCredentials.loginBody(username, password)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		JsonNode tokenResponse = jsonMapper.readTree(responseBody);
		return TestTokens.bearer(tokenResponse.get("accessToken").asString());
	}

	private String loginAsUser() throws Exception {
		return login(TestCredentials.USER_USERNAME, TestCredentials.USER_PASSWORD);
	}

	private String loginAsAdmin() throws Exception {
		return login(TestCredentials.ADMIN_USERNAME, TestCredentials.ADMIN_PASSWORD);
	}

	private ResultActions getAs(String bearer, String path) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.get(path).header(HttpHeaders.AUTHORIZATION, bearer));
	}

	private void stubPeoplePage() {
		String body = SwapiFixtures.page(SwapiFixtures.firstItem("people-page"), 10, 82, 9);
		stubJson(SWAPI_PEOPLE_PAGE_URL, 200, body);
	}

	private double metricCount(String adminBearer, String metricWithTag) throws Exception {
		MvcResult result = getAs(adminBearer, "/actuator/metrics/" + metricWithTag).andReturn();
		if (result.getResponse().getStatus() == 404) {
			return 0.0;
		}
		double count = 0.0;
		for (JsonNode measurement : jsonMapper.readTree(result.getResponse().getContentAsString()).get("measurements")) {
			if (COUNT_STATISTIC.equals(measurement.get("statistic").asString())) {
				count += measurement.get("value").asDouble();
			}
		}
		return count;
	}

	@Test
	void loginThenListPeopleCallsSwapiWithExpandedPagination() throws Exception {
		stubPeoplePage();
		String userBearer = loginAsUser();

		getAs(userBearer, PEOPLE_PATH + PEOPLE_PAGE_QUERY)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(10))
			.andExpect(jsonPath("$.content[0].id").value("1"))
			.andExpect(jsonPath("$.content[0].name").value("Luke Skywalker"))
			.andExpect(jsonPath("$.content[0].height").value(172))
			.andExpect(jsonPath("$.page").value(2))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.totalElements").value(82))
			.andExpect(jsonPath("$.totalPages").value(9));

		wireMock.verify(1, getRequestedFor(urlEqualTo(SWAPI_PEOPLE_PAGE_URL)));
	}

	@Test
	void loginReturnsBearerTokenWithTheConfiguredLifetime() throws Exception {
		mockMvc.perform(post(LOGIN_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(TestCredentials.userLoginBody()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(jwtProperties.expiration().toSeconds()))
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void filmsSearchCallsSwapiWithTitleAndNeverWithName() throws Exception {
		String body = SwapiFixtures.list(SwapiFixtures.firstItem("films"), 1, "title", "A New Hope ");
		stubJson("/films?title=hope", 200, body);
		String userBearer = loginAsUser();

		getAs(userBearer, "/api/v1/films?name=hope")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].title").value("A New Hope 1"));

		wireMock.verify(1, getRequestedFor(urlEqualTo("/films?title=hope")));
		wireMock.verify(0, getRequestedFor(urlEqualTo("/films?name=hope")));
	}

	@Test
	void unknownIdIsNotFoundAndRepeatingItDoesNotCallSwapiAgain() throws Exception {
		stubJson("/people/9999", 404, "{\"message\":\"not found\"}");
		String userBearer = loginAsUser();

		getAs(userBearer, PEOPLE_PATH + "/9999")
			.andExpect(status().isNotFound())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE));
		getAs(userBearer, PEOPLE_PATH + "/9999").andExpect(status().isNotFound());

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/9999")));
	}

	@Test
	void persistentSwapiServerErrorIsBadGatewayAfterThreeCalls() throws Exception {
		stubJson("/people/1", 500, "{}");
		String userBearer = loginAsUser();

		getAs(userBearer, PEOPLE_PATH + "/1")
			.andExpect(status().isBadGateway())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE));

		wireMock.verify(3, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void rateLimitedSwapiIsServiceUnavailableWithRetryAfterAndIsNotRetried() throws Exception {
		long resetEpochSeconds = Instant.now().plusSeconds(RATE_LIMIT_RESET_SECONDS).getEpochSecond();
		wireMock.stubFor(get(urlEqualTo("/people/1")).willReturn(aResponse().withStatus(429)
			.withHeader("x-ratelimit-reset", String.valueOf(resetEpochSeconds))));
		String userBearer = loginAsUser();

		MvcResult result = getAs(userBearer, PEOPLE_PATH + "/1")
			.andExpect(status().isServiceUnavailable())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
			.andReturn();

		String retryAfter = result.getResponse().getHeader(HttpHeaders.RETRY_AFTER);
		assertThat(retryAfter).isNotNull();
		assertThat(Long.parseLong(retryAfter)).isBetween(RETRY_AFTER_LOWER_BOUND_SECONDS,
				RETRY_AFTER_UPPER_BOUND_SECONDS);
		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

	@Test
	void adminEvictionMakesTheSameQueryCallSwapiAgain() throws Exception {
		stubPeoplePage();
		String adminBearer = loginAsAdmin();
		getAs(adminBearer, PEOPLE_PATH + PEOPLE_PAGE_QUERY).andExpect(status().isOk());
		getAs(adminBearer, PEOPLE_PATH + PEOPLE_PAGE_QUERY).andExpect(status().isOk());
		wireMock.verify(1, getRequestedFor(urlEqualTo(SWAPI_PEOPLE_PAGE_URL)));

		mockMvc.perform(delete("/api/v1/admin/cache").header(HttpHeaders.AUTHORIZATION, adminBearer))
			.andExpect(status().isNoContent());
		getAs(adminBearer, PEOPLE_PATH + PEOPLE_PAGE_QUERY).andExpect(status().isOk());

		wireMock.verify(2, getRequestedFor(urlEqualTo(SWAPI_PEOPLE_PAGE_URL)));
	}

	@Test
	void unauthorizedResponseCarriesGeneratedRequestId() throws Exception {
		String requestId = mockMvc.perform(MockMvcRequestBuilders.get(PEOPLE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
			.andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);

		assertThat(requestId).isNotBlank();
	}

	@Test
	void unauthorizedResponseEchoesIncomingValidRequestId() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(PEOPLE_PATH)
				.header(CorrelationIdFilter.HEADER_NAME, REQUEST_ID_SENT_BY_CLIENT))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(CorrelationIdFilter.HEADER_NAME, REQUEST_ID_SENT_BY_CLIENT));
	}

	@ParameterizedTest(name = "login ignores a {0} bearer")
	@EnumSource(TestTokens.Invalid.class)
	void loginIgnoresInvalidBearer(TestTokens.Invalid invalidToken) throws Exception {
		mockMvc.perform(post(LOGIN_PATH)
				.header(HttpHeaders.AUTHORIZATION, invalidToken.bearer(jwtProperties))
				.contentType(MediaType.APPLICATION_JSON)
				.content(TestCredentials.userLoginBody()))
			.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "a protected endpoint rejects a {0} bearer")
	@EnumSource(TestTokens.Invalid.class)
	void protectedEndpointRejectsInvalidBearer(TestTokens.Invalid invalidToken) throws Exception {
		getAs(invalidToken.bearer(jwtProperties), PEOPLE_PATH).andExpect(status().isUnauthorized());
	}

	@Test
	void adminSeesCacheAndHttpMetricsGrowAndResilienceMetricsExist() throws Exception {
		stubPeoplePage();
		String adminBearer = loginAsAdmin();
		double cacheGetsBefore = metricCount(adminBearer, CACHE_GETS_METRIC);
		double peopleRequestsBefore = metricCount(adminBearer, PEOPLE_REQUESTS_METRIC);

		getAs(adminBearer, PEOPLE_PATH + PEOPLE_PAGE_QUERY).andExpect(status().isOk());

		assertThat(metricCount(adminBearer, CACHE_GETS_METRIC) - cacheGetsBefore).isEqualTo(1.0);
		assertThat(metricCount(adminBearer, PEOPLE_REQUESTS_METRIC) - peopleRequestsBefore).isEqualTo(1.0);
		getAs(adminBearer, "/actuator/metrics")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.names").value(hasItem("resilience4j.circuitbreaker.state")));
	}

}
