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

/**
 * A pet of an owner.
 */
@Schema(name = "Pet", description = "A pet, always owned by exactly one owner")
public record PetResponse(@Schema(example = "1") Integer id, @Schema(example = "Leo") String name,
		@Schema(example = "2010-09-07") LocalDate birthDate, @Schema(description = "Pet type") PetTypeResponse type) {
}
