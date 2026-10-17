package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiVehicleProperties;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;

/**
 * Maps SWAPI Vehicle items to the domain.
 */
public final class VehicleMapper implements SwapiMapper<SwapiVehicleProperties, Vehicle> {

	@Override
	public Vehicle toDomain(SwapiItem<SwapiVehicleProperties> item) {
		SwapiVehicleProperties p = item.properties();
		return new Vehicle(item.uid(), p.name(), p.model(), p.manufacturer(), p.vehicleClass(),
				SwapiValues.toLong(p.costInCredits()), SwapiValues.toDouble(p.length()),
				SwapiValues.toLong(p.crew()), SwapiValues.toLong(p.passengers()),
				SwapiValues.toLong(p.cargoCapacity()), SwapiValues.toLong(p.maxAtmospheringSpeed()),
				p.consumables(), SwapiValues.idsFromUrls(p.pilots()), SwapiValues.idsFromUrls(p.films()));
	}

}
