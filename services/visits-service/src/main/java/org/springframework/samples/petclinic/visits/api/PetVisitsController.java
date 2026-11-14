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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.samples.petclinic.visits.api.dto.VisitResponse;
import org.springframework.samples.petclinic.visits.application.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bulk lookup used by the gateway to compose an owner with its pets and visits.
 */
@RestController
@RequestMapping("/pets/visits")
@Tag(name = "Visits", description = "Visits booked for a pet of an owner")
public class PetVisitsController {

	private final VisitService visits;

	public PetVisitsController(VisitService visits) {
		this.visits = visits;
	}

	@GetMapping
	@Operation(summary = "Visits of several pets in one call",
			description = "Used by the gateway composition; the order is by date.",
			responses = @ApiResponse(responseCode = "200", description = "Visits of the requested pets"))
	public List<VisitResponse> findByPetIds(
			@Parameter(description = "Comma separated pet ids", example = "7,8") @RequestParam List<Integer> petId) {
		return this.visits.findByPets(petId);
	}

}
