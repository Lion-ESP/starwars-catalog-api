package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiBadResponseException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiCircuitOpenException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiConnectionException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiRateLimitedException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiServerErrorException;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiTimeoutException;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogItemNotFoundException;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest extends CatalogWebTestSupport {

	private static final String PEOPLE_URL = "/api/v1/people";

	@Test
	@DisplayName("E33: SwapiServerErrorException responde 502 Problem Details")
	void serverErrorIsBadGateway() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenThrow(new SwapiServerErrorException(500));

		mockMvc.perform(get(PEOPLE_URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isBadGateway())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(502))
				.andExpect(jsonPath("$.instance").value(PEOPLE_URL));
	}

	@Test
	@DisplayName("SwapiBadResponseException responde 502 Problem Details")
	void badResponseIsBadGateway() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenThrow(new SwapiBadResponseException("unexpected body", null));

		mockMvc.perform(get(PEOPLE_URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isBadGateway())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	@DisplayName("E36: timeout responde 503 con Retry-After")
	void timeoutIsServiceUnavailable() throws Exception {
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenThrow(new SwapiTimeoutException(null, Duration.ofSeconds(30)));

		mockMvc.perform(get(PEOPLE_URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
				.andExpect(jsonPath("$.status").value(503));
	}

	@Test
	@DisplayName("E51: conexión rechazada responde 503 con Retry-After")
	void connectionFailureIsServiceUnavailable() throws Exception {
		when(peopleService.get("1")).thenThrow(new SwapiConnectionException(null, Duration.ofSeconds(30)));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"));
	}

	@Test
	@DisplayName("E37: cupo agotado responde 503 con Retry-After del reset")
	void rateLimitIsServiceUnavailable() throws Exception {
		when(peopleService.get("1")).thenThrow(new SwapiRateLimitedException(Duration.ofSeconds(12)));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "12"));
	}

	@Test
	@DisplayName("E38: breaker abierto responde 503 con Retry-After")
	void openCircuitIsServiceUnavailable() throws Exception {
		when(peopleService.get("1")).thenThrow(new SwapiCircuitOpenException(null, Duration.ofSeconds(30)));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"));
	}

	@ParameterizedTest(name = "Retry-After de {0} ms se informa como {1} s")
	@CsvSource({"0,1", "1,1", "500,1", "1000,1", "1001,2", "29200,30"})
	void retryAfterIsRoundedUpWithMinimumOneSecond(long millis, String expectedSeconds) throws Exception {
		when(peopleService.get("1")).thenThrow(new SwapiRateLimitedException(Duration.ofMillis(millis)));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, expectedSeconds));
	}

	@Test
	@DisplayName("E25: CatalogItemNotFoundException responde 404 Problem Details con el id")
	void notFoundNamesTheId() throws Exception {
		when(peopleService.get("9999")).thenThrow(new CatalogItemNotFoundException("9999"));

		mockMvc.perform(get(PEOPLE_URL + "/9999").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Not Found"));
	}

	@ParameterizedTest(name = "name longer than 100 characters is rejected on {0}")
	@ValueSource(strings = {"people", "films", "starships", "vehicles"})
	void overlongNameIsRejectedWithTheParameterName(String resource) throws Exception {
		String overlongName = "a".repeat(101);

		mockMvc.perform(get("/api/v1/" + resource).param("name", overlongName)
						.header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.parameter").value("name"))
				.andExpect(jsonPath("$.detail").value(containsString("name")));
	}

	@Test
	@DisplayName("name of exactly 100 characters is accepted")
	void nameOfMaximumLengthIsAccepted() throws Exception {
		String maximumLengthName = "a".repeat(100);
		when(peopleService.list(any(CatalogFilter.class), any(PageQuery.class)))
				.thenReturn(new Page<>(List.of(), 1, 10, 0, 0));

		mockMvc.perform(get(PEOPLE_URL).param("name", maximumLengthName)
						.header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("an unsupported method answers 405 Problem Details with the Allow header")
	void unsupportedMethodIsMethodNotAllowed() throws Exception {
		mockMvc.perform(post(PEOPLE_URL).header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(header().exists(HttpHeaders.ALLOW))
				.andExpect(jsonPath("$.status").value(405));
	}

	@Test
	@DisplayName("an unknown route with a valid token answers 404 Problem Details")
	void unknownRouteIsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/nothing-here").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("an unexpected exception answers a generic 500 without internal details")
	void unexpectedExceptionIsInternalServerErrorWithoutDetails() throws Exception {
		when(peopleService.get("1")).thenThrow(new IllegalStateException("secret internal state"));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken()))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(content().string(not(containsString("secret internal state"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	@Test
	@DisplayName("an unexpected exception is logged as an error with the stack and the request id")
	void unexpectedExceptionIsLoggedAsErrorWithRequestId(CapturedOutput output) throws Exception {
		when(peopleService.get("1")).thenThrow(new IllegalStateException("secret internal state"));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken())
				.header(CorrelationIdFilter.HEADER_NAME, "req-500"));

		assertThat(output.getAll()).contains("ERROR").contains("req-500")
				.contains("java.lang.IllegalStateException: secret internal state");
	}

	@Test
	@DisplayName("a 502 is logged as a warning with the request id")
	void badGatewayIsLoggedAsWarningWithRequestId(CapturedOutput output) throws Exception {
		when(peopleService.get("1")).thenThrow(new SwapiServerErrorException(500));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken())
				.header(CorrelationIdFilter.HEADER_NAME, "req-502"));

		assertThat(output.getAll()).contains("WARN").contains("req-502").contains("SwapiServerErrorException");
	}

	@Test
	@DisplayName("a 503 is logged as a warning with the request id")
	void serviceUnavailableIsLoggedAsWarningWithRequestId(CapturedOutput output) throws Exception {
		when(peopleService.get("1")).thenThrow(new SwapiRateLimitedException(Duration.ofSeconds(12)));

		mockMvc.perform(get(PEOPLE_URL + "/1").header(HttpHeaders.AUTHORIZATION, userToken())
				.header(CorrelationIdFilter.HEADER_NAME, "req-503"));

		assertThat(output.getAll()).contains("WARN").contains("req-503").contains("SwapiRateLimitedException");
	}
}
