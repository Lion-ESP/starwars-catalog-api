package io.github.lionesp.starwarscatalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata and the bearer security scheme.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	/** Name of the bearer security scheme referenced by the controllers. */
	public static final String BEARER_AUTH = "bearerAuth";

	/**
	 * Describes the API and its JWT bearer scheme.
	 *
	 * @return the OpenAPI definition
	 */
	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Star Wars Catalog API")
						.version("1.0.0")
						.description("Read-only catalog of Star Wars people, films, starships and vehicles, "
								+ "backed by SWAPI. Get a token from POST /api/v1/auth/login."))
				.components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}

}
