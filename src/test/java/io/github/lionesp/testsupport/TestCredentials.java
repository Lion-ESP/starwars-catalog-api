package io.github.lionesp.testsupport;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Demo users configured in {@code application.yml} and the signing secret shared by the tests that boot a context.
 */
public final class TestCredentials {

	/** Signing secret for the JWTs of every test context; longer than the 32 bytes HS256 needs. */
	public static final String JWT_SECRET = "shared-test-secret-with-more-than-32-bytes";

	/** Property that carries {@link #JWT_SECRET} into a {@code @SpringBootTest}. */
	public static final String JWT_SECRET_PROPERTY = "app.security.jwt.secret=" + JWT_SECRET;

	/** Username of the demo user that only has the USER role. */
	public static final String USER_USERNAME = "user";

	/** Password of the demo user that only has the USER role. */
	public static final String USER_PASSWORD = "user-password";

	/** Username of the demo user that has the USER and ADMIN roles. */
	public static final String ADMIN_USERNAME = "admin";

	/** Password of the demo user that has the USER and ADMIN roles. */
	public static final String ADMIN_PASSWORD = "admin-password";

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private TestCredentials() {
	}

	/**
	 * Builds the JSON body of a login request.
	 *
	 * @param username value of the {@code username} field, or {@code null} to leave the field out
	 * @param password value of the {@code password} field, or {@code null} to leave the field out
	 * @return the serialized JSON body
	 */
	public static String loginBody(String username, String password) {
		ObjectNode body = MAPPER.createObjectNode();
		if (username != null) {
			body.put("username", username);
		}
		if (password != null) {
			body.put("password", password);
		}
		return body.toString();
	}

	/**
	 * Builds the login body of the USER demo user.
	 *
	 * @return the serialized JSON body
	 */
	public static String userLoginBody() {
		return loginBody(USER_USERNAME, USER_PASSWORD);
	}

	/**
	 * Builds the login body of the ADMIN demo user.
	 *
	 * @return the serialized JSON body
	 */
	public static String adminLoginBody() {
		return loginBody(ADMIN_USERNAME, ADMIN_PASSWORD);
	}

}
