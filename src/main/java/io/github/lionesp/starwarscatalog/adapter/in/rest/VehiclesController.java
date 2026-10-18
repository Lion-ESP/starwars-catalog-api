package io.github.lionesp.starwarscatalog.adapter.in.rest;

import io.github.lionesp.starwarscatalog.adapter.in.rest.dto.PageResponse;
import io.github.lionesp.starwarscatalog.application.CatalogService;
import io.github.lionesp.starwarscatalog.config.OpenApiConfig;
import io.github.lionesp.starwarscatalog.domain.catalog.CatalogFilter;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;
import io.github.lionesp.starwarscatalog.domain.page.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoints of the vehicles catalog.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Read-only catalog of Star Wars vehicles")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class VehiclesController {

	private final CatalogService<Vehicle> service;

	/**
	 * Creates the controller.
	 *
	 * @param service use cases of the resource
	 */
	public VehiclesController(CatalogService<Vehicle> service) {
		this.service = service;
	}

	/**
	 * Lists vehicles.
	 *
	 * @param page 1-based page number
	 * @param size page size
	 * @param id   exact id filter
	 * @param name Case-insensitive partial name filter
	 * @return the requested page
	 */
	@GetMapping
	@Operation(summary = "List vehicles", description = "Paginated listing with optional id and name filters.")
	@ApiResponse(responseCode = "200", description = "Requested page")
	@ApiResponse(responseCode = "400", description = "Invalid page, size, id or name (longer than 100 characters)",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid token",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = "The token does not have the USER or ADMIN role",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "SWAPI failed or answered something unexpected",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "503", description = "SWAPI is temporarily unavailable",
			headers = @Header(name = "Retry-After", description = "Seconds to wait before retrying",
					schema = @Schema(implementation = Integer.class)),
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	public PageResponse<Vehicle> list(
			@Parameter(description = "1-based page number, at least 1 (default 1)")
			@RequestParam(required = false) Integer page,
			@Parameter(description = "Page size, between 1 and 100 (default 10)")
			@RequestParam(required = false) Integer size,
			@Parameter(description = "Exact id, a positive integer")
			@RequestParam(required = false) String id,
			@Parameter(description = "Case-insensitive partial name (blank is ignored, up to 100 characters)")
			@RequestParam(required = false) String name) {
		return PageResponse.from(service.list(CatalogFilter.of(id, name), PageQuery.of(page, size)));
	}

	/**
	 * Fetches one item.
	 *
	 * @param id item id
	 * @return the item
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Get a vehicle by id")
	@ApiResponse(responseCode = "200", description = "The item")
	@ApiResponse(responseCode = "400", description = "Invalid id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid token",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = "The token does not have the USER or ADMIN role",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "No item with that id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "SWAPI failed or answered something unexpected",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "503", description = "SWAPI is temporarily unavailable",
			headers = @Header(name = "Retry-After", description = "Seconds to wait before retrying",
					schema = @Schema(implementation = Integer.class)),
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	public Vehicle get(@PathVariable String id) {
		return service.get(id);
	}

}
