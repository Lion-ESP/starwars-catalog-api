package io.github.lionesp.starwarscatalog.adapter.out.swapi;

/**
 * The SWAPI resources exposed by the catalog and what varies between them.
 */
public enum SwapiResource {

	/** People. */
	PEOPLE("people", "name", true),

	/** Films, which SWAPI neither paginates nor searches by {@code name}. */
	FILMS("films", "title", false),

	/** Starships. */
	STARSHIPS("starships", "name", true),

	/** Vehicles. */
	VEHICLES("vehicles", "name", true);

	private final String path;

	private final String searchParam;

	private final boolean paginated;

	SwapiResource(String path, String searchParam, boolean paginated) {
		this.path = path;
		this.searchParam = searchParam;
		this.paginated = paginated;
	}

	/**
	 * Returns the path segment of the resource.
	 *
	 * @return the path, without slashes
	 */
	public String path() {
		return path;
	}

	/**
	 * Returns the query parameter SWAPI uses to search this resource by name.
	 *
	 * @return {@code name} or {@code title}
	 */
	public String searchParam() {
		return searchParam;
	}

	/**
	 * Tells whether SWAPI paginates this resource.
	 *
	 * @return {@code true} when {@code page} and {@code limit} are honored
	 */
	public boolean paginated() {
		return paginated;
	}

}
