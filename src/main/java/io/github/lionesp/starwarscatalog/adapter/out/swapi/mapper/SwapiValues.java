package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.Set;

/**
 * Pure functions that convert SWAPI text values into typed values.
 */
public final class SwapiValues {

	/** Texts SWAPI uses for a missing value; they map to {@code null}. */
	static final Set<String> UNKNOWN_TOKENS = Set.of("unknown", "n/a", "none", "");

	private static final Pattern INTEGER = Pattern.compile("-?(\\d+|\\d{1,3}(,\\d{3})+)");

	private static final Pattern DECIMAL = Pattern.compile("-?(\\d+|\\d{1,3}(,\\d{3})+)(\\.\\d+)?");

	private SwapiValues() {
	}

	private static String clean(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (UNKNOWN_TOKENS.contains(trimmed.toLowerCase(Locale.ROOT))) {
			return null;
		}
		return trimmed;
	}

	/**
	 * Parses an integer value, ignoring thousands separators and surrounding spaces.
	 *
	 * @param raw raw text
	 * @return the value, or {@code null} if unknown or not parseable
	 */
	public static Long toLong(String raw) {
		String cleaned = clean(raw);
		if (cleaned == null || !INTEGER.matcher(cleaned).matches()) {
			return null;
		}
		try {
			return Long.valueOf(cleaned.replace(",", ""));
		}
		catch (NumberFormatException overflow) {
			return null;
		}
	}

	/**
	 * Parses a decimal value, ignoring thousands separators and surrounding spaces.
	 *
	 * @param raw raw text
	 * @return the value, or {@code null} if unknown or not parseable
	 */
	public static Double toDouble(String raw) {
		String cleaned = clean(raw);
		if (cleaned == null || !DECIMAL.matcher(cleaned).matches()) {
			return null;
		}
		double value = Double.parseDouble(cleaned.replace(",", ""));
		return Double.isFinite(value) ? value : null;
	}

	/**
	 * Parses an ISO-8601 date.
	 *
	 * @param raw raw text
	 * @return the date, or {@code null} if it does not parse
	 */
	public static LocalDate toDate(String raw) {
		if (raw == null) {
			return null;
		}
		try {
			return LocalDate.parse(raw.trim());
		}
		catch (DateTimeParseException invalid) {
			return null;
		}
	}

	/**
	 * Extracts the last path segment of a SWAPI URL.
	 *
	 * @param url resource URL
	 * @return the id, or {@code null} if the URL is null or blank or has no last segment
	 */
	public static String idFromUrl(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		String trimmed = url.trim();
		String withoutTrailingSlash = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
		String id = withoutTrailingSlash.substring(withoutTrailingSlash.lastIndexOf('/') + 1);
		return id.isBlank() ? null : id;
	}

	/**
	 * Extracts the ids of a list of SWAPI URLs.
	 *
	 * @param urls resource URLs
	 * @return the ids, leaving out the URLs without one; empty if {@code urls} is null
	 */
	public static List<String> idsFromUrls(List<String> urls) {
		if (urls == null) {
			return List.of();
		}
		return urls.stream().map(SwapiValues::idFromUrl).filter(Objects::nonNull).toList();
	}

}
