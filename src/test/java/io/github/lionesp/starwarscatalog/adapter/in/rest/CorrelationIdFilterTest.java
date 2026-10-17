package io.github.lionesp.starwarscatalog.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	private record Outcome(String responseHeader, String mdcDuringRequest, MockHttpServletResponse response) {
	}

	private Outcome run(String incomingHeader) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		if (incomingHeader != null) {
			request.addHeader(CorrelationIdFilter.HEADER_NAME, incomingHeader);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> mdcSeenByChain = new AtomicReference<>();
		MockFilterChain chain = new MockFilterChain() {
			@Override
			public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
				mdcSeenByChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));
			}
		};
		filter.doFilter(request, response, chain);
		return new Outcome(response.getHeader(CorrelationIdFilter.HEADER_NAME), mdcSeenByChain.get(), response);
	}

	@Test
	void E39_validIncomingHeaderIsEchoedAndLogged() throws Exception {
		Outcome outcome = run("abc-123_X.y");

		assertThat(outcome.responseHeader()).isEqualTo("abc-123_X.y");
		assertThat(outcome.mdcDuringRequest()).isEqualTo("abc-123_X.y");
	}

	@Test
	void E39_missingHeaderGeneratesUuid() throws Exception {
		Outcome outcome = run(null);

		assertThat(UUID.fromString(outcome.responseHeader())).isNotNull();
		assertThat(outcome.mdcDuringRequest()).isEqualTo(outcome.responseHeader());
	}

	@Test
	void E39_mdcIsCleanAfterTheRequest() throws Exception {
		run("abc");

		assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void E53_headerLongerThan64IsReplacedByUuid() throws Exception {
		String tooLong = "a".repeat(65);

		Outcome outcome = run(tooLong);

		assertThat(outcome.responseHeader()).isNotEqualTo(tooLong);
		assertThat(UUID.fromString(outcome.responseHeader())).isNotNull();
		assertThat(outcome.mdcDuringRequest()).isEqualTo(outcome.responseHeader());
	}

	@Test
	void E53_headerOfExactly64IsAccepted() throws Exception {
		String atLimit = "a".repeat(64);

		assertThat(run(atLimit).responseHeader()).isEqualTo(atLimit);
	}

	@ParameterizedTest
	@ValueSource(strings = {"has space", "semi;colon", "inject\r\nX-Evil: 1", "line\nfeed", "carriage\rreturn", "ñandú", ""})
	void E53_headerWithDisallowedCharactersIsReplacedByUuid(String invalidHeader) throws Exception {
		Outcome outcome = run(invalidHeader);

		assertThat(outcome.responseHeader()).isNotEqualTo(invalidHeader);
		assertThat(UUID.fromString(outcome.responseHeader())).isNotNull();
		assertThat(outcome.mdcDuringRequest()).isEqualTo(outcome.responseHeader());
	}
}
