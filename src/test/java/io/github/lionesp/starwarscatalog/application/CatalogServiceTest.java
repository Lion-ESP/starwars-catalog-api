package io.github.lionesp.starwarscatalog.application;

import static io.github.lionesp.starwarscatalog.application.FakeCatalogPort.person;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogItemNotFoundException;
import io.github.lionesp.starwarscatalog.domain.exception.InvalidParameterException;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {

	private final Person luke = person("1", "Luke Skywalker");
	private final Person anakin = person("4", "Anakin Skywalker");
	private final Person vader = person("5", "Darth Vader");
	private final FakeCatalogPort port = new FakeCatalogPort(List.of(luke, anakin, vader));
	private final CatalogService<Person> service = new CatalogService<>(port);

	@Test
	void withoutFiltersDelegatesPage() {
		PageQuery pageTwoSizeOne = new PageQuery(2, 1);

		Page<Person> result = service.list(CatalogFilter.of(null, null), pageTwoSizeOne);

		assertThat(port.calls).containsExactly("findPage:2:1");
		assertThat(result.content()).containsExactly(anakin);
		assertThat(result.page()).isEqualTo(2);
		assertThat(result.size()).isEqualTo(1);
		assertThat(result.totalElements()).isEqualTo(3);
		assertThat(result.totalPages()).isEqualTo(3);
	}

	@Test
	void blankNameBehavesAsNoFilter() {
		CatalogFilter blankName = CatalogFilter.of(null, "   ");

		service.list(blankName, PageQuery.of(null, null));

		assertThat(port.calls).containsExactly("findPage:1:10");
	}

	@Test
	void nameFilterPaginatesInMemory() {
		CatalogFilter skyFragment = CatalogFilter.of(null, "sky");

		Page<Person> result = service.list(skyFragment, new PageQuery(1, 1));

		assertThat(port.calls).containsExactly("findAllByName:sky");
		assertThat(result.content()).containsExactly(luke);
		assertThat(result.totalElements()).isEqualTo(2);
		assertThat(result.totalPages()).isEqualTo(2);
	}

	@Test
	void nameWithoutMatchesReturnsEmptyPage() {
		CatalogFilter noMatchName = CatalogFilter.of(null, "zzz");

		Page<Person> result = service.list(noMatchName, PageQuery.of(null, null));

		assertThat(result.content()).isEmpty();
		assertThat(result.totalElements()).isZero();
		assertThat(result.totalPages()).isZero();
	}

	@Test
	void idFilterFindsById() {
		CatalogFilter existingId = CatalogFilter.of("1", null);

		Page<Person> result = service.list(existingId, PageQuery.of(null, null));

		assertThat(port.calls).containsExactly("findById:1");
		assertThat(result.content()).containsExactly(luke);
		assertThat(result.page()).isEqualTo(1);
		assertThat(result.size()).isEqualTo(10);
		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.totalPages()).isEqualTo(1);
	}

	@Test
	void idFilterMissingReturnsEmptyPage() {
		CatalogFilter missingId = CatalogFilter.of("9999", null);

		Page<Person> result = service.list(missingId, PageQuery.of(null, null));

		assertThat(result.content()).isEmpty();
		assertThat(result.totalElements()).isZero();
		assertThat(result.totalPages()).isZero();
	}

	@Test
	void idAndMatchingNameReturnsItem() {
		CatalogFilter idWithUppercaseName = CatalogFilter.of("1", "LUKE");

		Page<Person> result = service.list(idWithUppercaseName, PageQuery.of(null, null));

		assertThat(result.content()).containsExactly(luke);
		assertThat(port.calls).containsExactly("findById:1");
	}

	@Test
	void idAndNonMatchingNameReturnsEmptyPage() {
		CatalogFilter idWithOtherName = CatalogFilter.of("1", "vader");

		Page<Person> result = service.list(idWithOtherName, PageQuery.of(null, null));

		assertThat(result.content()).isEmpty();
		assertThat(result.totalElements()).isZero();
		assertThat(port.calls).containsExactly("findById:1");
	}

	@Test
	void idFilterPastEndKeepsTotals() {
		CatalogFilter existingId = CatalogFilter.of("1", null);

		Page<Person> result = service.list(existingId, new PageQuery(2, 10));

		assertThat(result.content()).isEmpty();
		assertThat(result.page()).isEqualTo(2);
		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.totalPages()).isEqualTo(1);
	}

	@Test
	void getReturnsEntity() {
		Person found = service.get("1");

		assertThat(found).isEqualTo(luke);
	}

	@Test
	void getMissingThrowsNotFound() {
		assertThatThrownBy(() -> service.get("9999")).isInstanceOf(CatalogItemNotFoundException.class);
	}

	@Test
	void getInvalidIdThrowsInvalidParameter() {
		assertThatThrownBy(() -> service.get("abc"))
				.isInstanceOfSatisfying(InvalidParameterException.class,
						exception -> assertThat(exception.parameter()).isEqualTo("id"));
	}

}
