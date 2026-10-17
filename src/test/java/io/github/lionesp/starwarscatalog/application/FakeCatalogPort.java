package io.github.lionesp.starwarscatalog.application;

import io.github.lionesp.starwarscatalog.application.port.out.CatalogPort;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import io.github.lionesp.starwarscatalog.domain.page.InMemoryPaginator;
import io.github.lionesp.starwarscatalog.domain.page.Page;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

class FakeCatalogPort implements CatalogPort<Person> {

	final List<String> calls = new ArrayList<>();
	private final List<Person> people;

	FakeCatalogPort(List<Person> people) {
		this.people = people;
	}

	static Person person(String id, String name) {
		return new Person(id, name, null, null, null, null, null, null, null, null, null, null, null);
	}

	@Override
	public Page<Person> findPage(PageQuery query) {
		calls.add("findPage:" + query.page() + ":" + query.size());
		return InMemoryPaginator.paginate(people, query);
	}

	@Override
	public List<Person> findAllByName(String name) {
		calls.add("findAllByName:" + name);
		return people.stream()
				.filter(p -> p.name().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
				.toList();
	}

	@Override
	public Optional<Person> findById(String id) {
		calls.add("findById:" + id);
		return people.stream().filter(p -> p.id().equals(id)).findFirst();
	}

}
