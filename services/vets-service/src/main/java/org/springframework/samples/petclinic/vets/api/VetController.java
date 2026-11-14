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
package org.springframework.samples.petclinic.vets.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetsResponse;
import org.springframework.samples.petclinic.vets.application.VetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vets API (read-only).
 */
@RestController
@RequestMapping("/vets")
@Tag(name = "Vets", description = "Veterinarians and their specialties")
public class VetController {

	private final VetService vets;

	public VetController(VetService vets) {
		this.vets = vets;
	}

	@GetMapping
	@Operation(summary = "Paginated list of vets",
			description = "Out-of-range pages are served as page 1 and reported in the payload (ADR-004).",
			responses = { @ApiResponse(responseCode = "200", description = "A page of vets"),
					@ApiResponse(responseCode = "400", description = "Invalid page or size",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
	public PageResponse<VetResponse> findPage(
			@Parameter(description = "1-based page number", example = "1") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size, 1..100", example = "5") @RequestParam(defaultValue = "5") int size) {
		return this.vets.findPage(page, size);
	}

	@GetMapping("/all")
	@Operation(summary = "Every vet in a single object",
			description = "Keeps the legacy contract: { \"vetList\": [...] } (BR-VET-02).",
			responses = @ApiResponse(responseCode = "200",
					description = "All vets, ordered by last name, wrapped in an object"))
	public VetsResponse findAll() {
		return this.vets.findAll();
	}

}
