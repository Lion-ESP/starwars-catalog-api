package io.github.lionesp.starwarscatalog.adapter.out.swapi;

import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the {@link RestClient} that talks to SWAPI.
 */
@Configuration(proxyBeanMethods = false)
public class SwapiClientConfig {

	/**
	 * Builds the SWAPI {@link RestClient} with the configured base URL and timeouts.
	 *
	 * @param properties SWAPI settings
	 * @return the client
	 */
	@Bean
	public RestClient swapiRestClient(SwapiProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
	}

	/**
	 * Provides the clock used to compute {@code Retry-After} values.
	 *
	 * @return the system UTC clock
	 */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
