package io.github.lionesp.starwarscatalog.domain.model;

import java.util.List;

/**
 * A vehicle. Unknown numeric values are {@code null}.
 *
 * @param id                   SWAPI uid
 * @param name                 name
 * @param model                model
 * @param manufacturer         manufacturer
 * @param vehicleClass         class of vehicle
 * @param costInCredits        cost in credits
 * @param length               length in meters
 * @param crew                 crew size
 * @param passengers           passenger capacity
 * @param cargoCapacity        cargo capacity in kilograms
 * @param maxAtmospheringSpeed maximum speed in atmosphere
 * @param consumables          time the vehicle can sustain its crew
 * @param pilotIds             ids of the pilots
 * @param filmIds              ids of the films
 */
public record Vehicle(String id, String name, String model, String manufacturer, String vehicleClass,
		Long costInCredits, Double length, Long crew, Long passengers, Long cargoCapacity,
		Long maxAtmospheringSpeed, String consumables, List<String> pilotIds, List<String> filmIds)
		implements CatalogItem {

	/**
	 * Validates the record, copying the lists defensively ({@code null} becomes empty).
	 */
	public Vehicle {
		pilotIds = pilotIds == null ? List.of() : List.copyOf(pilotIds);
		filmIds = filmIds == null ? List.of() : List.copyOf(filmIds);
	}

}
