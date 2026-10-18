package io.github.lionesp.starwarscatalog.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class ProblemDetailsWriterTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	@DisplayName("a request URI with characters that are not valid in a URI does not break the write")
	void invalidRequestUriIsOmittedInsteadOfFailing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bad path|{x}");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

		ProblemDetailsWriter.write(jsonMapper, request, response, problem);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(jsonMapper.readTree(response.getContentAsString()).path("instance").isString()).isFalse();
	}

	@Test
	@DisplayName("a valid request URI is written as the instance")
	void validRequestUriIsWrittenAsInstance() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/people");
		MockHttpServletResponse response = new MockHttpServletResponse();

		ProblemDetailsWriter.write(jsonMapper, request, response, ProblemDetail.forStatus(HttpStatus.FORBIDDEN));

		assertThat(response.getContentAsString()).contains("\"instance\":\"/api/v1/people\"");
	}
}
