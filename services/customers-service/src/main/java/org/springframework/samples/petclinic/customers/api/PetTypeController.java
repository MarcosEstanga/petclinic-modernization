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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.samples.petclinic.customers.api.dto.PetTypeResponse;
import org.springframework.samples.petclinic.customers.application.PetTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pet types catalog.
 */
@RestController
@RequestMapping("/petTypes")
@Tag(name = "Pet types", description = "Catalog of pet types")
public class PetTypeController {

	private final PetTypeService types;

	public PetTypeController(PetTypeService types) {
		this.types = types;
	}

	@GetMapping
	@Operation(summary = "List every pet type, ordered by name",
			responses = @ApiResponse(responseCode = "200", description = "All pet types, alphabetically ordered"))
	public List<PetTypeResponse> findAll() {
		return this.types.findAll();
	}

}
