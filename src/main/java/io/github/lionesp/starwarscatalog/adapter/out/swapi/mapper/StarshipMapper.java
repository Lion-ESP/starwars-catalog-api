package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiStarshipProperties;
import io.github.lionesp.starwarscatalog.domain.model.Starship;

/**
 * Maps SWAPI Starship items to the domain.
 */
public final class StarshipMapper implements SwapiMapper<SwapiStarshipProperties, Starship> {

	@Override
	public Starship toDomain(SwapiItem<SwapiStarshipProperties> item) {
		SwapiStarshipProperties p = item.properties();
		return new Starship(item.uid(), p.name(), p.model(), p.manufacturer(), p.starshipClass(),
				SwapiValues.toLong(p.costInCredits()), SwapiValues.toDouble(p.length()),
				SwapiValues.toLong(p.crew()), SwapiValues.toLong(p.passengers()),
				SwapiValues.toLong(p.cargoCapacity()), SwapiValues.toLong(p.maxAtmospheringSpeed()),
				SwapiValues.toDouble(p.hyperdriveRating()), SwapiValues.toLong(p.mglt()), p.consumables(),
				SwapiValues.idsFromUrls(p.pilots()), SwapiValues.idsFromUrls(p.films()));
	}

}
