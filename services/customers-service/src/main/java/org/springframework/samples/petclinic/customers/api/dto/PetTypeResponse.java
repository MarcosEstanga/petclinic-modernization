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

/**
 * Kind of pet (cat, dog, ...).
 */
@Schema(name = "PetType", description = "Catalog entry describing the kind of a pet")
public record PetTypeResponse(@Schema(example = "1") Integer id, @Schema(example = "cat") String name) {
}
