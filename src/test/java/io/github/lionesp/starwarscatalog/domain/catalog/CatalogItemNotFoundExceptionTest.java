package io.github.lionesp.starwarscatalog.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogItemNotFoundExceptionTest {

	@Test
	void carriesTheSearchedIdInTheAccessorAndTheMessage() {
		CatalogItemNotFoundException exception = new CatalogItemNotFoundException("9999");

		assertThat(exception.id()).isEqualTo("9999");
		assertThat(exception.getMessage()).contains("9999");
	}

}
