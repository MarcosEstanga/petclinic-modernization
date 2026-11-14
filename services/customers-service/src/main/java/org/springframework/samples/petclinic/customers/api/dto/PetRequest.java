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
package org.springframework.samples.petclinic.customers.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload to create or update a pet.
 * <p>
 * {@code typeId} is required when creating and optional when updating, mirroring the
 * legacy {@code PetValidator} (BR-PET-02). {@code birthDate} is mandatory and must not be
 * in the future (BR-PET-03).
 */
@Schema(name = "PetRequest", description = "Pet data. typeId is mandatory on create, optional on update")
public record PetRequest(
		@Schema(description = "Pet name, unique per owner (case-insensitive)", example = "Leo",
				maxLength = 30) @NotBlank @Size(max = 30) String name,

		@Schema(description = "Birth date, today or earlier", example = "2010-09-07") @NotNull LocalDate birthDate,

		@Schema(description = "Pet type id", example = "1") Integer typeId) {
}
