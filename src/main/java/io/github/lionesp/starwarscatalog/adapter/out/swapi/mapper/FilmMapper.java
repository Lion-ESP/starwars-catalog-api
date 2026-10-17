package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiFilmProperties;
import io.github.lionesp.starwarscatalog.domain.model.Film;

/**
 * Maps SWAPI Film items to the domain.
 */
public final class FilmMapper implements SwapiMapper<SwapiFilmProperties, Film> {

	@Override
	public Film toDomain(SwapiItem<SwapiFilmProperties> item) {
		SwapiFilmProperties p = item.properties();
		return new Film(item.uid(), p.title(), p.episodeId(), p.director(), p.producer(),
				SwapiValues.toDate(p.releaseDate()), p.openingCrawl(), SwapiValues.idsFromUrls(p.characters()),
				SwapiValues.idsFromUrls(p.planets()), SwapiValues.idsFromUrls(p.starships()),
				SwapiValues.idsFromUrls(p.vehicles()), SwapiValues.idsFromUrls(p.species()));
	}

}
