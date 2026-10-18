package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static io.github.lionesp.testsupport.TestCredentials.ADMIN_PASSWORD;
import static io.github.lionesp.testsupport.TestCredentials.ADMIN_USERNAME;
import static io.github.lionesp.testsupport.TestCredentials.USER_PASSWORD;
import static io.github.lionesp.testsupport.TestCredentials.USER_USERNAME;
import static io.github.lionesp.testsupport.TestCredentials.loginBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.lionesp.starwarscatalog.security.JwtProperties;
import io.github.lionesp.starwarscatalog.security.ProblemDetailsAccessDeniedHandler;
import io.github.lionesp.starwarscatalog.security.ProblemDetailsAuthenticationEntryPoint;
import io.github.lionesp.starwarscatalog.security.SecurityConfig;
import io.github.lionesp.starwarscatalog.security.TokenService;
import io.github.lionesp.starwarscatalog.security.UsersProperties;
import io.github.lionesp.testsupport.TestCredentials;
import io.github.lionesp.testsupport.TestTokens;
import java.time.Clock;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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

@SpringBootTest(classes = AuthControllerTest.TestApplication.class, properties = TestCredentials.JWT_SECRET_PROPERTY)
@AutoConfigureMockMvc
class AuthControllerTest {

	private static final String LOGIN_URL = "/api/v1/auth/login";
	private static final String PEOPLE_URL = "/api/v1/people";
	private static final String HEALTH_URL = "/actuator/health";
	private static final int MAX_CREDENTIAL_LENGTH = 72;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtProperties jwtProperties;

	static Stream<Arguments> invalidLoginBodies() {
		return Stream.of(
				Arguments.of(loginBody(null, USER_PASSWORD), "username", "missing username"),
				Arguments.of(loginBody(USER_USERNAME, null), "password", "missing password"),
				Arguments.of(loginBody("  ", USER_PASSWORD), "username", "blank username"),
				Arguments.of(loginBody(USER_USERNAME, ""), "password", "blank password"));
	}

	@Test
	void validCredentialsReturnBearerTokenWithTheConfiguredLifetime() throws Exception {
		String responseBody = login(USER_USERNAME, USER_PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value(TokenService.BEARER_SCHEME))
				.andExpect(jsonPath("$.expiresIn").value(jwtProperties.expiration().toSeconds()))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		String accessToken = JsonPath.read(responseBody, "$.accessToken");
		var decoded = jwtDecoder.decode(accessToken);
		assertThat(decoded.getSubject()).isEqualTo(USER_USERNAME);
		assertThat(decoded.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactly("USER");
	}

	@Test
	void adminTokenCarriesBothRoles() throws Exception {
		String responseBody = login(ADMIN_USERNAME, ADMIN_PASSWORD)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String accessToken = JsonPath.read(responseBody, "$.accessToken");
		assertThat(jwtDecoder.decode(accessToken).getClaimAsStringList(TokenService.ROLES_CLAIM))
				.containsExactlyInAnyOrder("USER", "ADMIN");
	}

	@Test
	void wrongPasswordAndUnknownUserGiveTheSameDetail() throws Exception {
		String wrongPasswordDetail = detailOf(login(USER_USERNAME, "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));
		String unknownUserDetail = detailOf(login("ghost", "whatever")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));

		assertThat(wrongPasswordDetail).isNotBlank().isEqualTo(unknownUserDetail);
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("invalidLoginBodies")
	void invalidLoginBodyNamesTheInvalidField(String body, String invalidField) throws Exception {
		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value(containsString(invalidField)));
	}

	@Test
	void catalogWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get(PEOPLE_URL))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@ParameterizedTest(name = "{0} token")
	@EnumSource(TestTokens.Invalid.class)
	void catalogWithInvalidTokenIsUnauthorized(TestTokens.Invalid invalidToken) throws Exception {
		mockMvc.perform(get(PEOPLE_URL).header(HttpHeaders.AUTHORIZATION, invalidToken.bearer(jwtProperties)))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@ParameterizedTest(name = "login with a {0} token")
	@EnumSource(TestTokens.Invalid.class)
	void loginIgnoresInvalidBearerToken(TestTokens.Invalid invalidToken) throws Exception {
		mockMvc.perform(post(LOGIN_URL)
						.header(HttpHeaders.AUTHORIZATION, invalidToken.bearer(jwtProperties))
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(USER_USERNAME, USER_PASSWORD)))
				.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "health with a {0} token")
	@EnumSource(TestTokens.Invalid.class)
	void healthIgnoresInvalidBearerToken(TestTokens.Invalid invalidToken) throws Exception {
		mockMvc.perform(get(HEALTH_URL).header(HttpHeaders.AUTHORIZATION, invalidToken.bearer(jwtProperties)))
				.andExpect(status().isOk());
	}

	@Test
	void failedLoginCarriesTheBearerChallenge() throws Exception {
		login(USER_USERNAME, "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, TokenService.BEARER_SCHEME));
	}

	@ParameterizedTest(name = "a login body that is {1}")
	@CsvSource(delimiter = '|', value = {
			"{bad|malformed",
			"|empty"
	})
	void unreadableLoginBodyIsBadRequestProblemDetails(String body) throws Exception {
		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(body == null ? "" : body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void unsupportedLoginContentTypeIsProblemDetails() throws Exception {
		mockMvc.perform(post(LOGIN_URL).contentType(MediaType.TEXT_PLAIN).content(USER_USERNAME))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@ParameterizedTest(name = "{0} longer than 72 characters is rejected naming the field")
	@ValueSource(strings = {"username", "password"})
	void overlongCredentialIsRejectedNamingTheField(String field) throws Exception {
		String overlong = "a".repeat(MAX_CREDENTIAL_LENGTH + 1);
		String username = "username".equals(field) ? overlong : USER_USERNAME;
		String password = "password".equals(field) ? overlong : USER_PASSWORD;

		login(username, password)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value(containsString(field)));
	}

	@Test
	void credentialOfMaximumLengthPassesValidation() throws Exception {
		login("u".repeat(MAX_CREDENTIAL_LENGTH), "p".repeat(MAX_CREDENTIAL_LENGTH))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions login(String username, String password) throws Exception {
		return mockMvc.perform(post(LOGIN_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody(username, password)));
	}

	private String detailOf(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.detail");
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
