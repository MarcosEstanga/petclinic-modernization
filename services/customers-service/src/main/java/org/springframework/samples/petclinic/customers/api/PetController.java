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
package org.springframework.samples.petclinic.customers.api;

import java.net.URI;

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
import org.springframework.samples.petclinic.customers.api.dto.PetRequest;
import org.springframework.samples.petclinic.customers.api.dto.PetResponse;
import org.springframework.samples.petclinic.customers.application.PetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pets API, always scoped by owner (a pet that belongs to another owner is a 404,
 * BR-PET-07).
 */
@RestController
@RequestMapping("/owners/{ownerId}/pets")
@Tag(name = "Pets", description = "Pets of an owner")
public class PetController {

	private final PetService pets;

	public PetController(PetService pets) {
		this.pets = pets;
	}

	@GetMapping("/{petId}")
	@Operation(summary = "Find a pet of an owner",
			responses = { @ApiResponse(responseCode = "200", description = "The pet"),
					@ApiResponse(responseCode = "404", description = "Owner or pet not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public PetResponse findById(@Parameter(description = "Owner id", example = "1") @PathVariable Integer ownerId,
			@Parameter(description = "Pet id", example = "1") @PathVariable Integer petId) {
		return this.pets.findById(ownerId, petId);
	}

	@PostMapping
	@Operation(summary = "Add a pet to an owner",
			description = "The pet name is unique per owner (case-insensitive). typeId is mandatory.",
			responses = {
					@ApiResponse(responseCode = "201", description = "Pet created",
							headers = @Header(name = "Location", description = "URI of the new pet")),
					@ApiResponse(responseCode = "400", description = "Validation failed or unknown pet type",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "404", description = "Owner not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "409", description = "Pet name already used by this owner",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public ResponseEntity<PetResponse> create(
			@Parameter(description = "Owner id", example = "1") @PathVariable Integer ownerId,
			@Valid @RequestBody PetRequest request) {
		PetResponse created = this.pets.create(ownerId, request);
		return ResponseEntity.created(URI.create("/owners/" + ownerId + "/pets/" + created.id())).body(created);
	}

	@PutMapping("/{petId}")
	@Operation(summary = "Update a pet",
			description = "typeId may be omitted to keep the current type; reusing its own name is allowed.",
			responses = { @ApiResponse(responseCode = "200", description = "Pet updated"),
					@ApiResponse(responseCode = "400", description = "Validation failed",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "404", description = "Owner or pet not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "409", description = "Name already used by another pet of this owner",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public PetResponse update(@Parameter(description = "Owner id", example = "1") @PathVariable Integer ownerId,
			@Parameter(description = "Pet id", example = "1") @PathVariable Integer petId,
			@Valid @RequestBody PetRequest request) {
		return this.pets.update(ownerId, petId, request);
	}

}
