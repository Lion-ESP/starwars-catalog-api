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
	void E26_plainIntegerIsParsed() {
		assertThat(SwapiValues.toLong("172")).isEqualTo(172L);
	}

	@Test
	void E26_thousandsSeparatorIsRemoved() {
		assertThat(SwapiValues.toLong("843,342")).isEqualTo(843342L);
	}

	@Test
	void E26_valueBeyondIntRangeIsParsedAsLong() {
		assertThat(SwapiValues.toLong("1000000000000")).isEqualTo(1_000_000_000_000L);
	}

	@Test
	void E27_trailingSpaceIsIgnored() {
		assertThat(SwapiValues.toDouble("36.8 ")).isEqualTo(36.8);
	}

	@Test
	void E27_decimalWithZeroFractionIsParsed() {
		assertThat(SwapiValues.toDouble("4.0")).isEqualTo(4.0);
	}

	@Test
	void E27_thousandsSeparatorIsRemovedInDecimals() {
		assertThat(SwapiValues.toDouble("1,358")).isEqualTo(1358.0);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "unknown", "n/a", "none", " ", "30-165" })
	void E28_unknownTokensRangesAndBlanksBecomeNull(String unknownValue) {
		assertThat(SwapiValues.toLong(unknownValue)).isNull();
		assertThat(SwapiValues.toDouble(unknownValue)).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc", "12abc", "1.2.3", "--5" })
	void E48_nonNumericTextBecomesNullWithoutException(String notANumber) {
		assertThat(SwapiValues.toLong(notANumber)).isNull();
		assertThat(SwapiValues.toDouble(notANumber)).isNull();
	}

	@Test
	void E48_decimalTextInIntegerFieldBecomesNull() {
		assertThat(SwapiValues.toLong("36.8")).isNull();
	}

	@Test
	void E30_isoDateIsParsed() {
		assertThat(SwapiValues.toDate("1977-05-25")).isEqualTo(LocalDate.of(1977, 5, 25));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "25/05/1977", "not a date", "1977-13-45" })
	void E48_unparseableDateBecomesNull(String badDate) {
		assertThat(SwapiValues.toDate(badDate)).isNull();
	}

	@Test
	void E29_idIsTheLastSegmentOfTheUrl() {
		assertThat(SwapiValues.idFromUrl("https://www.swapi.tech/api/films/1")).isEqualTo("1");
	}

	@Test
	void E29_idIgnoresTrailingSlash() {
		assertThat(SwapiValues.idFromUrl("https://www.swapi.tech/api/planets/42/")).isEqualTo("42");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "  " })
	void E29_nullOrBlankUrlHasNoId(String missingUrl) {
		assertThat(SwapiValues.idFromUrl(missingUrl)).isNull();
	}

	@Test
	void E29_nullUrlListBecomesEmptyList() {
		assertThat(SwapiValues.idsFromUrls(null)).isEmpty();
	}

	@Test
	void E29_urlListBecomesIdList() {
		List<String> filmUrls = List.of("https://www.swapi.tech/api/films/1", "https://www.swapi.tech/api/films/5");

		assertThat(SwapiValues.idsFromUrls(filmUrls)).containsExactly("1", "5");
	}

}
