package io.github.lionesp.starwarscatalog.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credentials sent to the login endpoint. BCrypt only considers the first 72 bytes, so longer values are rejected.
 *
 * @param username login name
 * @param password plain-text password
 */
@Schema(description = "Credentials of a user")
public record LoginRequest(
		@Schema(description = "Login name", example = "user") @NotBlank @Size(max = 72) String username,
		@Schema(description = "Plain-text password", example = "user-password") @NotBlank @Size(max = 72) String password) {
}
