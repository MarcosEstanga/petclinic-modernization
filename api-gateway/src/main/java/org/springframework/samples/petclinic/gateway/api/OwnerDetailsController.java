/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.gateway.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.samples.petclinic.gateway.api.dto.OwnerDetailsView;
import org.springframework.samples.petclinic.gateway.application.OwnerDetailsComposer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner detail composition endpoint.
 */
@RestController
@RequestMapping("/api/owners")
@Tag(name = "Composition", description = "Cross-service views")
public class OwnerDetailsController {

	private final OwnerDetailsComposer composer;

	public OwnerDetailsController(OwnerDetailsComposer composer) {
		this.composer = composer;
	}

	@GetMapping("/{ownerId}/details")
	@Operation(summary = "Owner with its pets and the visits of every pet",
			description = "Replaces the legacy ownerDetails page: one call returns everything the page showed.",
			responses = { @ApiResponse(responseCode = "200", description = "The composed owner"),
					@ApiResponse(responseCode = "404", description = "Owner not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "503", description = "A dependency is unavailable",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public OwnerDetailsView details(@Parameter(description = "Owner id", example = "6") @PathVariable Integer ownerId) {
		return this.composer.compose(ownerId);
	}

}
