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
package org.springframework.samples.petclinic.gateway.api.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Owner with its pets and each pet's visits: the composition the legacy
 * {@code ownerDetails} view rendered in a single page.
 */
@Schema(name = "OwnerDetails", description = "Owner, its pets and the visits of every pet")
public record OwnerDetailsView(@Schema(example = "6") Integer id, @Schema(example = "Jean") String firstName,
		@Schema(example = "Coleman") String lastName, @Schema(example = "105 N. Lake St.") String address,
		@Schema(example = "Monona") String city, @Schema(example = "6085552654") String telephone,
		@Schema(description = "Pets, ordered by name, each one with its visits ordered by date") List<PetView> pets) {

	/**
	 * A pet inside the composition.
	 *
	 * @param id pet id
	 * @param name pet name
	 * @param birthDate birth date
	 * @param type kind of pet
	 * @param visits visits of this pet, oldest first
	 */
	@Schema(name = "OwnerPet")
	public record PetView(@Schema(example = "7") Integer id, @Schema(example = "Samantha") String name,
			@Schema(example = "2012-09-04") LocalDate birthDate, @Schema(description = "Kind of pet") PetTypeView type,
			@Schema(description = "Visits of this pet, ordered by date") List<VisitView> visits) {
	}

	/**
	 * Kind of pet.
	 *
	 * @param id pet type id
	 * @param name pet type name
	 */
	@Schema(name = "OwnerPetType")
	public record PetTypeView(@Schema(example = "1") Integer id, @Schema(example = "cat") String name) {
	}

	/**
	 * A visit inside the composition.
	 *
	 * @param id visit id
	 * @param date visit date
	 * @param description what happened
	 */
	@Schema(name = "OwnerPetVisit")
	public record VisitView(@Schema(example = "1") Integer id, @Schema(example = "2013-01-01") LocalDate date,
			@Schema(example = "rabies shot") String description) {
	}

}
