package io.github.lionesp.starwarscatalog.adapter.in.rest;

import io.github.lionesp.starwarscatalog.application.CacheAdminService;
import io.github.lionesp.starwarscatalog.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative endpoints.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Administrative operations")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminController {

	private final CacheAdminService cacheAdminService;

	/**
	 * Creates the controller.
	 *
	 * @param cacheAdminService cache administration use cases
	 */
	public AdminController(CacheAdminService cacheAdminService) {
		this.cacheAdminService = cacheAdminService;
	}

	/**
	 * Evicts every cached SWAPI response.
	 */
	@DeleteMapping("/cache")
	@Operation(summary = "Evict the SWAPI response cache")
	@ApiResponse(responseCode = "204", description = "Cache evicted")
	@ApiResponse(responseCode = "401", description = "Missing or invalid token",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = "The token does not have the ADMIN role",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void evictCache() {
		cacheAdminService.evictAll();
	}

}
