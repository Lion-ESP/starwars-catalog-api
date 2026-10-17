package io.github.lionesp.starwarscatalog.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * In-memory users bound from {@code app.security.users}.
 *
 * @param users users allowed to log in
 */
@Validated
@ConfigurationProperties("app.security")
public record UsersProperties(@DefaultValue List<@Valid User> users) {

	/**
	 * A user allowed to log in.
	 *
	 * @param username     login name
	 * @param passwordHash BCrypt hash of the password
	 * @param roles        roles without the {@code ROLE_} prefix, for example {@code USER}
	 */
	public record User(@NotBlank String username, @NotBlank String passwordHash, @NotEmpty List<String> roles) {
	}
}
