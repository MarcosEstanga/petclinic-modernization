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
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.customers.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.customers.api.dto.OwnerRequest;
import org.springframework.samples.petclinic.customers.api.dto.OwnerSummary;
import org.springframework.samples.petclinic.customers.application.OwnerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owners API.
 */
@RestController
@RequestMapping("/owners")
@Tag(name = "Owners", description = "Search, register and update pet owners")
public class OwnerController {

	private final OwnerService owners;

	public OwnerController(OwnerService owners) {
		this.owners = owners;
	}

	@GetMapping
	@Operation(summary = "Search owners by last-name prefix",
			description = "Case-insensitive prefix search. A blank last name returns every owner. "
					+ "Out-of-range pages are served as page 1 and reported in the payload (ADR-004).",
			responses = { @ApiResponse(responseCode = "200", description = "A page of owners"),
					@ApiResponse(responseCode = "400", description = "Invalid page or size",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public PageResponse<OwnerSummary> search(
			@Parameter(description = "Last-name prefix",
					example = "Franklin") @RequestParam(required = false) String lastName,
			@Parameter(description = "1-based page number", example = "1") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size, 1..100", example = "5") @RequestParam(defaultValue = "5") int size) {
		return this.owners.search(lastName, page, size);
	}

	@GetMapping("/{ownerId}")
	@Operation(summary = "Find an owner by id, including its pets",
			responses = { @ApiResponse(responseCode = "200", description = "The owner"),
					@ApiResponse(responseCode = "404", description = "Owner not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public OwnerDetails findById(@Parameter(description = "Owner id", example = "1") @PathVariable Integer ownerId) {
		return this.owners.findById(ownerId);
	}

	@PostMapping
	@Operation(summary = "Register a new owner",
			responses = {
					@ApiResponse(responseCode = "201", description = "Owner created",
							headers = @Header(name = "Location", description = "URI of the new owner")),
					@ApiResponse(responseCode = "400", description = "Validation failed",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public ResponseEntity<OwnerDetails> create(@Valid @RequestBody OwnerRequest request) {
		OwnerDetails created = this.owners.create(request);
		return ResponseEntity.created(URI.create("/owners/" + created.id())).body(created);
	}

	@PutMapping("/{ownerId}")
	@Operation(summary = "Update an owner (the id always comes from the URL)",
			responses = { @ApiResponse(responseCode = "200", description = "Owner updated"),
					@ApiResponse(responseCode = "400", description = "Validation failed",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "404", description = "Owner not found",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public OwnerDetails update(@Parameter(description = "Owner id", example = "1") @PathVariable Integer ownerId,
			@Valid @RequestBody OwnerRequest request) {
		return this.owners.update(ownerId, request);
	}

}
