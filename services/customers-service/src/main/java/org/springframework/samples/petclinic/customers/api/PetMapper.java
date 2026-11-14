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

import org.springframework.samples.petclinic.customers.api.dto.PetResponse;
import org.springframework.samples.petclinic.customers.api.dto.PetTypeResponse;
import org.springframework.samples.petclinic.customers.domain.Pet;
import org.springframework.samples.petclinic.customers.domain.PetType;

/**
 * Maps the pet aggregate to HTTP representations. Entities never leave the service
 * boundary.
 */
public final class PetMapper {

	private PetMapper() {
	}

	public static PetResponse toResponse(Pet pet) {
		return new PetResponse(pet.getId(), pet.getName(), pet.getBirthDate(), toResponse(pet.getType()));
	}

	public static PetTypeResponse toResponse(PetType type) {
		return new PetTypeResponse(type.getId(), type.getName());
	}

}
