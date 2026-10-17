package io.github.lionesp.starwarscatalog.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials sent to the login endpoint.
 *
 * @param username login name
 * @param password plain-text password
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
