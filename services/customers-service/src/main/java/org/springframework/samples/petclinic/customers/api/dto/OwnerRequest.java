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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload to create or update an owner.
 * <p>
 * It deliberately has no {@code id} field: the identifier comes from the URL (or is
 * generated on create), so a client can never choose it (BR-OWN-10, BR-OWN-11).
 */
@Schema(name = "OwnerRequest", description = "Owner data. The identifier is never part of the body")
public record OwnerRequest(
		@Schema(description = "First name", example = "George",
				maxLength = 30) @NotBlank @Size(max = 30) String firstName,

		@Schema(description = "Last name", example = "Franklin",
				maxLength = 30) @NotBlank @Size(max = 30) String lastName,

		@Schema(description = "Street address", example = "110 W. Liberty St.",
				maxLength = 255) @NotBlank @Size(max = 255) String address,

		@Schema(description = "City", example = "Madison", maxLength = 80) @NotBlank @Size(max = 80) String city,

		@Schema(description = "Ten-digit telephone", example = "6085551023", pattern = "\\d{10}") @NotBlank @Pattern(
				regexp = "\\d{10}", message = "{telephone.invalid}") String telephone) {
}
