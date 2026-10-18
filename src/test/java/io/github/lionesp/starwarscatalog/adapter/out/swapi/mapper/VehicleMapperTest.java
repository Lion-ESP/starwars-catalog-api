package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.SwapiFixtures;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItemResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiVehicleProperties;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

class VehicleMapperTest {

	private final VehicleMapper mapper = new VehicleMapper();

	@Test
	void detailResultEnvelopeMapsToDomain() {
		SwapiItemResponse<SwapiVehicleProperties> response = SwapiFixtures.readFixture("vehicle-detail", new TypeReference<>() {
		});

		Vehicle vehicle = mapper.toDomain(response.result());

		Vehicle expectedSandCrawler = new Vehicle("4", "Sand Crawler", "Digger Crawler",
				"Corellia Mining Corporation", "wheeled", 150000L, 36.8, 46L, 30L, 50000L, 30L, "2 months",
				List.of(), List.of("1", "5"));
		assertThat(vehicle).isEqualTo(expectedSandCrawler);
	}

	@Test
	void unknownNumericFieldsBecomeNull() {
		SwapiVehicleProperties unknownValues = new SwapiVehicleProperties("V", "m", "mf", "cls", "unknown", "n/a",
				"none", "30-165", "", "abc", "c", List.of(), List.of());

		Vehicle vehicle = mapper.toDomain(new SwapiItem<>("5", unknownValues));

		assertThat(vehicle.costInCredits()).isNull();
		assertThat(vehicle.length()).isNull();
		assertThat(vehicle.crew()).isNull();
		assertThat(vehicle.passengers()).isNull();
		assertThat(vehicle.cargoCapacity()).isNull();
		assertThat(vehicle.maxAtmospheringSpeed()).isNull();
	}

}
