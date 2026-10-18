package io.github.lionesp.starwarscatalog.security;

/**
 * A token issued after a successful login.
 *
 * @param value            compact serialized JWT
 * @param expiresInSeconds lifetime of the token in seconds
 */
public record IssuedToken(String value, long expiresInSeconds) {
}
