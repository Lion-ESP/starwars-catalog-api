package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiPersonProperties;
import io.github.lionesp.starwarscatalog.domain.model.Person;

/**
 * Maps SWAPI Person items to the domain.
 */
public final class PersonMapper implements SwapiMapper<SwapiPersonProperties, Person> {

	@Override
	public Person toDomain(SwapiItem<SwapiPersonProperties> item) {
		SwapiPersonProperties p = item.properties();
		return new Person(item.uid(), p.name(), p.gender(), p.birthYear(), SwapiValues.toLong(p.height()),
				SwapiValues.toDouble(p.mass()), p.hairColor(), p.skinColor(), p.eyeColor(),
				SwapiValues.idFromUrl(p.homeworld()), SwapiValues.idsFromUrls(p.films()),
				SwapiValues.idsFromUrls(p.starships()), SwapiValues.idsFromUrls(p.vehicles()));
	}

}
