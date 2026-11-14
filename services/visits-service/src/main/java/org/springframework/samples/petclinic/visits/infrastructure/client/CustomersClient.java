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
package org.springframework.samples.petclinic.visits.infrastructure.client;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client for the part of customers-service this service needs.
 * <p>
 * It uses {@code @HttpExchange} over a {@code RestClient}, so the contract is a Java
 * interface and can be bound to a mock server in tests.
 */
@HttpExchange("/owners")
public interface CustomersClient {

	/**
	 * Reads a pet scoped by owner. A 404 means "this pet does not belong to this owner".
	 * @param ownerId the owner
	 * @param petId the pet
	 * @return the pet
	 */
	@GetExchange("/{ownerId}/pets/{petId}")
	PetSummary getPet(@PathVariable Integer ownerId, @PathVariable Integer petId);

	/**
	 * Minimal view of a pet.
	 *
	 * @param id the pet id
	 * @param name the pet name
	 */
	@Schema(name = "PetSummary")
	record PetSummary(Integer id, String name) {
	}

}
