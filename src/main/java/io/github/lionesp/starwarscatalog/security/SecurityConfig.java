package io.github.lionesp.starwarscatalog.security;

import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Stateless security setup with two filter chains.
 * <p>
 * The public chain has no resource server, so a bad bearer token sent to a public endpoint is ignored
 * instead of rejected. The protected chain validates JWTs and enforces roles.
 */
@Configuration
public class SecurityConfig {

	private static final String ROLE_USER = "USER";
	private static final String ROLE_ADMIN = "ADMIN";
	private static final String ROLE_PREFIX = "ROLE_";

	/**
	 * Chain for the endpoints that need no authentication.
	 *
	 * @param http builder provided by Spring Security
	 * @return the public filter chain
	 * @throws Exception if the chain cannot be built
	 */
	@Bean
	@Order(1)
	SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
		PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
		RequestMatcher publicEndpoints = new OrRequestMatcher(
				paths.matcher(HttpMethod.POST, "/api/v1/auth/login"),
				paths.matcher("/actuator/health"),
				paths.matcher("/swagger-ui.html"),
				paths.matcher("/swagger-ui/**"),
				paths.matcher("/v3/api-docs/**"));
		http.securityMatcher(publicEndpoints)
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return statelessDefaults(http).build();
	}

	/**
	 * Chain for every other request, authenticated with a bearer JWT.
	 *
	 * @param http          builder provided by Spring Security
	 * @param jwtDecoder    decoder that validates the bearer tokens
	 * @param entryPoint    handler for missing or invalid tokens
	 * @param deniedHandler handler for insufficient roles
	 * @return the protected filter chain
	 * @throws Exception if the chain cannot be built
	 */
	@Bean
	@Order(2)
	SecurityFilterChain protectedFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
			ProblemDetailsAuthenticationEntryPoint entryPoint, ProblemDetailsAccessDeniedHandler deniedHandler)
			throws Exception {
		http.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/api/v1/admin/**", "/actuator/metrics/**").hasRole(ROLE_ADMIN)
						.requestMatchers("/api/v1/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
						.anyRequest().denyAll())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(deniedHandler))
				.oauth2ResourceServer(resourceServer -> resourceServer
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(deniedHandler)
						.jwt(jwt -> jwt
								.decoder(jwtDecoder)
								.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return statelessDefaults(http).build();
	}

	private HttpSecurity statelessDefaults(HttpSecurity http) {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(TokenService.ROLES_CLAIM);
		authorities.setAuthorityPrefix(ROLE_PREFIX);
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	/**
	 * Token issuer and decoder factory.
	 *
	 * @param properties JWT settings
	 * @return the token service
	 */
	@Bean
	TokenService tokenService(JwtProperties properties) {
		return new TokenService(properties);
	}

	/**
	 * Decoder used by the resource server.
	 *
	 * @param tokenService token service that knows the key and the issuer
	 * @return the JWT decoder
	 */
	@Bean
	JwtDecoder jwtDecoder(TokenService tokenService) {
		return tokenService.decoder();
	}

	/**
	 * BCrypt encoder used to check the stored password hashes.
	 *
	 * @return the password encoder
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * In-memory user store built from the configured users.
	 *
	 * @param users configured users
	 * @return the user details service
	 */
	@Bean
	UserDetailsService userDetailsService(UsersProperties users) {
		List<UserDetails> details = users.users().stream()
				.map(user -> User.withUsername(user.username())
						.password(user.passwordHash())
						.roles(user.roles().toArray(String[]::new))
						.build())
				.toList();
		return new InMemoryUserDetailsManager(details);
	}

	/**
	 * Authentication manager used by the login endpoint.
	 *
	 * @param userDetailsService user store
	 * @param passwordEncoder    encoder that checks the hashes
	 * @return the authentication manager
	 */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
}
