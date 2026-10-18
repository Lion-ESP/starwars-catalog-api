package io.github.lionesp.testsupport;

/**
 * Property overrides shared by the tests that boot the full application context.
 */
public final class TestProperties {

	/** Points SWAPI to a port nobody listens on, so a test that forgets to stub it fails fast without network. */
	public static final String UNREACHABLE_SWAPI_BASE_URL_PROPERTY = "swapi.base-url=http://localhost:1/api";

	private TestProperties() {
	}

}
