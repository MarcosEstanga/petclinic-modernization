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

import org.springframework.samples.petclinic.vets.api.dto.SpecialtyResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetResponse;
import org.springframework.samples.petclinic.vets.domain.Specialty;
import org.springframework.samples.petclinic.vets.domain.Vet;

/**
 * Maps vet entities to HTTP representations.
 */
public final class VetMapper {

	private VetMapper() {
	}

	public static VetResponse toResponse(Vet vet) {
		return new VetResponse(vet.getId(), vet.getFirstName(), vet.getLastName(),
				vet.getSpecialties().stream().map(VetMapper::toResponse).toList(), vet.getNrOfSpecialties());
	}

	public static SpecialtyResponse toResponse(Specialty specialty) {
		return new SpecialtyResponse(specialty.getId(), specialty.getName());
	}

}
