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
package org.springframework.samples.petclinic.visits.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A booked visit.
 */
@Schema(name = "Visit", description = "A visit booked for a pet of an owner")
public record VisitResponse(@Schema(example = "1") Integer id, @Schema(example = "6") Integer ownerId,
		@Schema(example = "7") Integer petId, @Schema(example = "2026-12-01") LocalDate date,
		@Schema(example = "rabies shot") String description) {
}
