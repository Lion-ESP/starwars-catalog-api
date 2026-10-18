package io.github.lionesp.testsupport;

import io.github.lionesp.starwarscatalog.adapter.in.rest.AdminController;
import io.github.lionesp.starwarscatalog.adapter.in.rest.ApiExceptionHandler;
import io.github.lionesp.starwarscatalog.adapter.in.rest.CorrelationIdFilter;
import io.github.lionesp.starwarscatalog.adapter.in.rest.FilmsController;
import io.github.lionesp.starwarscatalog.adapter.in.rest.PeopleController;
import io.github.lionesp.starwarscatalog.adapter.in.rest.StarshipsController;
import io.github.lionesp.starwarscatalog.adapter.in.rest.VehiclesController;
import io.github.lionesp.starwarscatalog.security.JwtProperties;
import io.github.lionesp.starwarscatalog.security.ProblemDetailsAccessDeniedHandler;
import io.github.lionesp.starwarscatalog.security.ProblemDetailsAuthenticationEntryPoint;
import io.github.lionesp.starwarscatalog.security.SecurityConfig;
import io.github.lionesp.starwarscatalog.security.UsersProperties;
import java.time.Clock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Web context with the real security and the catalog controllers. It lives outside the application package so
 * the component scan of the full application never picks it up.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableConfigurationProperties({JwtProperties.class, UsersProperties.class})
@Import({SecurityConfig.class, PeopleController.class, FilmsController.class, StarshipsController.class,
		VehiclesController.class, AdminController.class, ApiExceptionHandler.class, CorrelationIdFilter.class,
		ProblemDetailsAuthenticationEntryPoint.class, ProblemDetailsAccessDeniedHandler.class})
public class WebTestApplication {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
