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
package org.springframework.samples.petclinic.visits.api;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.visits.api.dto.VisitRequest;
import org.springframework.samples.petclinic.visits.api.dto.VisitResponse;
import org.springframework.samples.petclinic.visits.application.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Visits of a pet, always scoped by owner.
 */
@RestController
@RequestMapping("/owners/{ownerId}/pets/{petId}/visits")
@Tag(name = "Visits", description = "Visits booked for a pet of an owner")
public class VisitController {

	private final VisitService visits;

	public VisitController(VisitService visits) {
		this.visits = visits;
	}

	@GetMapping
	@Operation(summary = "List the visits of a pet, oldest first",
			description = "The owner and pet are validated against customers-service.",
			responses = { @ApiResponse(responseCode = "200", description = "Visits ordered by date"),
					@ApiResponse(responseCode = "404", description = "Owner or pet not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "503", description = "customers-service is unavailable",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public List<VisitResponse> findByPet(
			@Parameter(description = "Owner id", example = "6") @PathVariable Integer ownerId,
			@Parameter(description = "Pet id", example = "7") @PathVariable Integer petId) {
		return this.visits.findByPet(ownerId, petId);
	}

	@PostMapping
	@Operation(summary = "Book a visit",
			description = "The date must be strictly in the future and the pet must belong to the owner.",
			responses = {
					@ApiResponse(responseCode = "201", description = "Visit booked",
							headers = @Header(name = "Location", description = "URI of the new visit")),
					@ApiResponse(responseCode = "400", description = "Validation failed or date not in the future",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "404", description = "Owner or pet not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "503", description = "customers-service is unavailable",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public ResponseEntity<VisitResponse> create(
			@Parameter(description = "Owner id", example = "6") @PathVariable Integer ownerId,
			@Parameter(description = "Pet id", example = "7") @PathVariable Integer petId,
			@Valid @RequestBody VisitRequest request) {
		VisitResponse created = this.visits.create(ownerId, petId, request);
		URI location = URI.create("/owners/" + ownerId + "/pets/" + petId + "/visits/" + created.id());
		return ResponseEntity.created(location).body(created);
	}

}
