package io.github.lionesp.starwarscatalog.adapter.out.swapi.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItem;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiItemResponse;
import io.github.lionesp.starwarscatalog.adapter.out.swapi.dto.SwapiVehicleProperties;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class VehicleMapperTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final VehicleMapper mapper = new VehicleMapper();

	@Test
	void E31_detailResultEnvelopeMapsToDomain() throws IOException {
		SwapiItemResponse<SwapiVehicleProperties> response;
		try (InputStream stream = getClass().getResourceAsStream("/swapi/vehicle-detail.json")) {
			response = JSON.readValue(stream, new TypeReference<>() {
			});
		}

		Vehicle vehicle = mapper.toDomain(response.result());

		Vehicle expectedSandCrawler = new Vehicle("4", "Sand Crawler", "Digger Crawler",
				"Corellia Mining Corporation", "wheeled", 150000L, 36.8, 46L, 30L, 50000L, 30L, "2 months",
				List.of(), List.of("1", "5"));
		assertThat(vehicle).isEqualTo(expectedSandCrawler);
	}

	@Test
	void E28_unknownNumericFieldsBecomeNull() {
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
