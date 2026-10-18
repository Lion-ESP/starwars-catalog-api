package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.exception.SwapiTimeoutException;
import io.github.lionesp.testsupport.SwapiResilienceTestApplication;
import io.github.lionesp.testsupport.SwapiWireMockSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SwapiResilienceTestApplication.class, properties = "swapi.read-timeout=300ms")
class SwapiReadTimeoutTest extends SwapiWireMockSupport {

	private static final int RESPONSE_DELAY_MILLIS = 3_000;

	@Autowired
	private SwapiClient client;

	@Test
	void readTimeoutIsNotRetried() {
		wireMock.stubFor(get(urlEqualTo("/people/1"))
			.willReturn(aResponse().withStatus(200).withFixedDelay(RESPONSE_DELAY_MILLIS)));

		assertThatThrownBy(() -> client.fetchById(SwapiResource.PEOPLE, SwapiPersonProperties.class, "1"))
			.isInstanceOf(SwapiTimeoutException.class);

		wireMock.verify(1, getRequestedFor(urlEqualTo("/people/1")));
	}

}
