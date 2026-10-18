package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;

import com.jayway.jsonpath.JsonPath;
import io.github.lionesp.starwarscatalog.security.JwtProperties;
import io.github.lionesp.starwarscatalog.security.ProblemDetailsAccessDeniedHandler;
import io.github.lionesp.starwarscatalog.security.ProblemDetailsAuthenticationEntryPoint;
import io.github.lionesp.starwarscatalog.security.SecurityConfig;
import io.github.lionesp.starwarscatalog.security.TokenService;
import io.github.lionesp.starwarscatalog.security.UsersProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(classes = AuthControllerTest.TestApplication.class, properties = {
		"app.security.jwt.secret=" + AuthControllerTest.SECRET
})
@AutoConfigureMockMvc
class AuthControllerTest {

	static final String SECRET = "integration-test-secret-with-more-than-32-bytes";
	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String PEOPLE_URL = "/api/v1/people";
	private static final String HEALTH_URL = "/actuator/health";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtProperties jwtProperties;

	@Test
	@DisplayName("E1: login correcto devuelve un JWT Bearer con la vigencia configurada")
	void validCredentialsReturnBearerToken() throws Exception {
		String responseBody = login("user", "user-password")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		String accessToken = JsonPath.read(responseBody, "$.accessToken");
		var decoded = jwtDecoder.decode(accessToken);
		assertThat(decoded.getSubject()).isEqualTo("user");
		assertThat(decoded.getClaimAsStringList("roles")).containsExactly("USER");
	}

	@Test
	@DisplayName("E1: el token del usuario admin lleva los roles USER y ADMIN")
	void adminTokenCarriesBothRoles() throws Exception {
		String responseBody = login("admin", "admin-password")
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String accessToken = JsonPath.read(responseBody, "$.accessToken");
		assertThat(jwtDecoder.decode(accessToken).getClaimAsStringList("roles")).containsExactlyInAnyOrder("USER", "ADMIN");
	}

	@Test
	@DisplayName("E2: contraseña incorrecta y usuario inexistente dan el mismo 401")
	void wrongPasswordAndUnknownUserGiveTheSameDetail() throws Exception {
		String wrongPasswordDetail = detailOf(login("user", "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));
		String unknownUserDetail = detailOf(login("ghost", "whatever")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));

		assertThat(wrongPasswordDetail).isNotBlank().isEqualTo(unknownUserDetail);
	}

	@ParameterizedTest(name = "E3: {0}")
	@CsvSource(delimiter = '|', value = {
			"sin username|{\"password\":\"user-password\"}|username",
			"sin password|{\"username\":\"user\"}|password",
			"username en blanco|{\"username\":\"  \",\"password\":\"user-password\"}|username",
			"password en blanco|{\"username\":\"user\",\"password\":\"\"}|password"
	})
	void invalidLoginBodyNamesTheInvalidField(String scenario, String body, String invalidField) throws Exception {
		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value(containsString(invalidField)));
	}

	@Test
	@DisplayName("E4: sin header Authorization el catálogo responde 401 Problem Details")
	void catalogWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get(PEOPLE_URL))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@ParameterizedTest(name = "E5: token {0}")
	@ValueSource(strings = {"expired", "badSignature", "malformed"})
	void catalogWithInvalidTokenIsUnauthorized(String kind) throws Exception {
		mockMvc.perform(get(PEOPLE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken(kind)))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@ParameterizedTest(name = "E42: login con token {0} responde 200")
	@ValueSource(strings = {"expired", "badSignature", "malformed"})
	void loginIgnoresInvalidBearerToken(String kind) throws Exception {
		mockMvc.perform(post(LOGIN_URL)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken(kind))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"user\",\"password\":\"user-password\"}"))
				.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "E42: health con token {0} responde 200")
	@ValueSource(strings = {"expired", "badSignature", "malformed"})
	void healthIgnoresInvalidBearerToken(String kind) throws Exception {
		mockMvc.perform(get(HEALTH_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken(kind)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("a failed login carries the Bearer challenge in WWW-Authenticate")
	void failedLoginCarriesTheBearerChallenge() throws Exception {
		login("user", "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

	@ParameterizedTest(name = "a login body that is {0} answers 400 Problem Details")
	@CsvSource(delimiter = '|', value = {
			"malformed|{bad",
			"empty|"
	})
	void unreadableLoginBodyIsBadRequestProblemDetails(String kind, String body) throws Exception {
		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(body == null ? "" : body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("a login with a non-JSON content type answers 415 Problem Details")
	void unsupportedLoginContentTypeIsProblemDetails() throws Exception {
		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.TEXT_PLAIN).content("user"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@ParameterizedTest(name = "{0} longer than 72 characters is rejected naming the field")
	@ValueSource(strings = {"username", "password"})
	void overlongCredentialIsRejectedNamingTheField(String field) throws Exception {
		String overlong = "a".repeat(73);
		String username = "username".equals(field) ? overlong : "user";
		String password = "password".equals(field) ? overlong : "user-password";

		login(username, password)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value(containsString(field)));
	}

	@Test
	@DisplayName("a credential of exactly 72 characters passes validation")
	void credentialOfMaximumLengthPassesValidation() throws Exception {
		login("u".repeat(72), "p".repeat(72)).andExpect(status().isUnauthorized());
	}

	private ResultActions login(String username, String password) throws Exception {
		String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
		return mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String detailOf(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.detail");
	}

	private String invalidToken(String kind) {
		return switch (kind) {
			case "expired" -> new TokenService(jwtProperties, Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC))
					.issue("user", List.of("USER"));
			case "badSignature" -> new TokenService(new JwtProperties(
					"some-other-secret-with-more-than-32-bytes", jwtProperties.issuer(), Duration.ofHours(1)), Clock.systemUTC())
					.issue("user", List.of("USER"));
			default -> "not.a.jwt";
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableConfigurationProperties({JwtProperties.class, UsersProperties.class})
	@Import({SecurityConfig.class, AuthController.class, ApiExceptionHandler.class,
			ProblemDetailsAuthenticationEntryPoint.class,
			ProblemDetailsAccessDeniedHandler.class})
	static class TestApplication {

		@Bean
		Clock clock() {
			return Clock.systemUTC();
		}
	}
}
