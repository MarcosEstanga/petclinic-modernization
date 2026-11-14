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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload to book a visit.
 * <p>
 * The date must be strictly in the future (BR-VIS-02) and the description is limited to
 * 255 characters (BR-VIS-01).
 */
@Schema(name = "VisitRequest", description = "Visit to book. The date must be strictly in the future")
public record VisitRequest(

		@Schema(description = "Visit date, tomorrow or later", example = "2026-12-01") @NotNull LocalDate date,

		@Schema(description = "What happened / will happen", example = "rabies shot",
				maxLength = 255) @NotBlank @Size(max = 255) String description) {
}
