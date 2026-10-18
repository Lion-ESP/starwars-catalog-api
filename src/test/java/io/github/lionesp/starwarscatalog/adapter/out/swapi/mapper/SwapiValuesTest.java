package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SwapiValuesTest {

	@Test
	void plainIntegerIsParsed() {
		assertThat(SwapiValues.toLong("172")).isEqualTo(172L);
	}

	@Test
	void thousandsSeparatorIsRemoved() {
		assertThat(SwapiValues.toLong("843,342")).isEqualTo(843342L);
	}

	@Test
	void valueBeyondIntRangeIsParsedAsLong() {
		assertThat(SwapiValues.toLong("1000000000000")).isEqualTo(1_000_000_000_000L);
	}

	@Test
	void trailingSpaceIsIgnored() {
		assertThat(SwapiValues.toDouble("36.8 ")).isEqualTo(36.8);
	}

	@Test
	void decimalWithZeroFractionIsParsed() {
		assertThat(SwapiValues.toDouble("4.0")).isEqualTo(4.0);
	}

	@Test
	void thousandsSeparatorIsRemovedInDecimals() {
		assertThat(SwapiValues.toDouble("1,358")).isEqualTo(1358.0);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "unknown", "n/a", "none", " ", "30-165" })
	void unknownTokensRangesAndBlanksBecomeNull(String unknownValue) {
		assertThat(SwapiValues.toLong(unknownValue)).isNull();
		assertThat(SwapiValues.toDouble(unknownValue)).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc", "12abc", "1.2.3", "--5" })
	void nonNumericTextBecomesNullWithoutException(String notANumber) {
		assertThat(SwapiValues.toLong(notANumber)).isNull();
		assertThat(SwapiValues.toDouble(notANumber)).isNull();
	}

	@Test
	void decimalTextInIntegerFieldBecomesNull() {
		assertThat(SwapiValues.toLong("36.8")).isNull();
	}

	@Test
	void isoDateIsParsed() {
		assertThat(SwapiValues.toDate("1977-05-25")).isEqualTo(LocalDate.of(1977, 5, 25));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "25/05/1977", "not a date", "1977-13-45" })
	void unparseableDateBecomesNull(String badDate) {
		assertThat(SwapiValues.toDate(badDate)).isNull();
	}

	@Test
	void idIsTheLastSegmentOfTheUrl() {
		assertThat(SwapiValues.idFromUrl("https://www.swapi.tech/api/films/1")).isEqualTo("1");
	}

	@Test
	void idIgnoresTrailingSlash() {
		assertThat(SwapiValues.idFromUrl("https://www.swapi.tech/api/planets/42/")).isEqualTo("42");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "  " })
	void nullOrBlankUrlHasNoId(String missingUrl) {
		assertThat(SwapiValues.idFromUrl(missingUrl)).isNull();
	}

	@Test
	void nullUrlListBecomesEmptyList() {
		assertThat(SwapiValues.idsFromUrls(null)).isEmpty();
	}

	@Test
	void urlListBecomesIdList() {
		List<String> filmUrls = List.of("https://www.swapi.tech/api/films/1", "https://www.swapi.tech/api/films/5");

		assertThat(SwapiValues.idsFromUrls(filmUrls)).containsExactly("1", "5");
	}

	@ParameterizedTest
	@ValueSource(strings = { "1,2,3", "1,5", "12,34", "1234,567", ",123", "1,234," })
	void misplacedThousandsSeparatorsBecomeNull(String misplacedSeparators) {
		assertThat(SwapiValues.toLong(misplacedSeparators)).isNull();
		assertThat(SwapiValues.toDouble(misplacedSeparators)).isNull();
	}

	@Test
	void groupedThousandsAreParsedInIntegersAndDecimals() {
		assertThat(SwapiValues.toLong("1,234,567")).isEqualTo(1_234_567L);
		assertThat(SwapiValues.toDouble("1,234.5")).isEqualTo(1234.5);
	}

	@Test
	void decimalBeyondDoubleRangeBecomesNull() {
		String beyondDouble = "1" + "0".repeat(400);

		assertThat(SwapiValues.toDouble(beyondDouble)).isNull();
	}

	@Test
	void slashOnlyUrlHasNoId() {
		assertThat(SwapiValues.idFromUrl("/")).isNull();
		assertThat(SwapiValues.idFromUrl("https://www.swapi.tech/api/films//")).isNull();
	}

	@Test
	void slashOnlyUrlIsDroppedFromTheIdList() {
		List<String> mixedUrls = List.of("/", "https://www.swapi.tech/api/films/2");

		assertThat(SwapiValues.idsFromUrls(mixedUrls)).containsExactly("2");
	}

}
