package io.github.lionesp.starwarscatalog;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.adapter.in.rest.CorrelationIdFilter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"app.security.jwt.secret=" + ProblemDetailsRealServerTest.SECRET
})
class ProblemDetailsRealServerTest {

	static final String SECRET = "real-server-test-secret-with-more-than-32-bytes";
	private static final String LOGIN_PATH = "/api/v1/auth/login";
	private static final String PEOPLE_PATH = "/api/v1/people";

	@LocalServerPort
	private int port;

	private final HttpClient client = HttpClient.newHttpClient();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private String validBearerToken;

	@BeforeEach
	void obtainValidToken() throws Exception {
		HttpResponse<String> login = send(post(LOGIN_PATH, "{\"username\":\"user\",\"password\":\"user-password\"}"));
		validBearerToken = "Bearer " + jsonMapper.readTree(login.body()).get("accessToken").asString();
	}

	@Test
	@DisplayName("a missing token answers 401 with the Bearer challenge on a real server")
	void missingTokenCarriesTheBearerChallenge() throws Exception {
		HttpResponse<String> response = send(request(PEOPLE_PATH).GET().build());

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).contains("Bearer");
		assertProblemJson(response);
	}

	@Test
	@DisplayName("an invalid token answers 401 with the Bearer challenge on a real server")
	void invalidTokenCarriesTheBearerChallenge() throws Exception {
		HttpResponse<String> response = send(request(PEOPLE_PATH)
				.header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt").GET().build());

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).contains("Bearer");
	}

	@Test
	@DisplayName("a failed login answers 401 with the Bearer challenge on a real server")
	void failedLoginCarriesTheBearerChallenge() throws Exception {
		HttpResponse<String> response = send(post(LOGIN_PATH, "{\"username\":\"user\",\"password\":\"wrong\"}"));

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).contains("Bearer");
		assertProblemJson(response);
	}

	@Test
	@DisplayName("a malformed login body answers 400 Problem Details on a real server")
	void malformedLoginBodyIsProblemDetails() throws Exception {
		HttpResponse<String> response = send(post(LOGIN_PATH, "{bad"));

		assertThat(response.statusCode()).isEqualTo(400);
		assertProblemJson(response);
	}

	@Test
	@DisplayName("an unsupported method on the login path answers 405 Problem Details on a real server")
	void unsupportedMethodOnLoginIsProblemDetails() throws Exception {
		HttpResponse<String> response = send(request(LOGIN_PATH).DELETE().build());

		assertThat(response.statusCode()).isEqualTo(405);
		assertProblemJson(response);
	}

	@Test
	@DisplayName("an unknown route with a valid token answers 404 Problem Details on a real server")
	void unknownRouteIsProblemDetails() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/nothing-here")
				.header(HttpHeaders.AUTHORIZATION, validBearerToken).GET().build());

		assertThat(response.statusCode()).isEqualTo(404);
		assertProblemJson(response);
	}

	@Test
	@DisplayName("the request id of the response is the one sent by the client")
	void requestIdIsEchoed() throws Exception {
		HttpResponse<String> response = send(request(PEOPLE_PATH)
				.header(CorrelationIdFilter.HEADER_NAME, "real-server-id").GET().build());

		assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME)).contains("real-server-id");
	}

	private void assertProblemJson(HttpResponse<String> response) throws Exception {
		assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(""))
				.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		JsonNode body = jsonMapper.readTree(response.body());
		assertThat(body.get("status").asInt()).isEqualTo(response.statusCode());
	}

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private HttpRequest post(String path, String body) {
		return request(path).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(BodyPublishers.ofString(body)).build();
	}

	private HttpResponse<String> send(HttpRequest request) throws Exception {
		return client.send(request, BodyHandlers.ofString());
	}
}
