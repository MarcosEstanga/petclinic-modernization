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
package org.springframework.samples.petclinic.visits.application;

import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.common.web.ServiceUnavailableException;
import org.springframework.samples.petclinic.visits.infrastructure.client.CustomersClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Port to the customers bounded context.
 * <p>
 * Translates the remote failures into the service's own semantics: a missing owner/pet
 * becomes a 404, and any other failure (timeout, connection refused, 5xx) becomes a 503 —
 * never a 500 (BR-VIS-03, ADR-005).
 */
@Component
public class CustomersGateway {

	private final CustomersClient client;

	public CustomersGateway(CustomersClient client) {
		this.client = client;
	}

	/**
	 * Fails unless the pet exists and belongs to the owner.
	 * @param ownerId the owner id
	 * @param petId the pet id
	 */
	public void ensurePetBelongsToOwner(Integer ownerId, Integer petId) {
		try {
			CustomersClient.PetSummary pet = this.client.getPet(ownerId, petId);
			if (pet == null || pet.id() == null) {
				throw notFound(ownerId, petId);
			}
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
				throw notFound(ownerId, petId);
			}
			throw new ServiceUnavailableException(
					"customers-service responded " + ex.getStatusCode() + " while validating the pet");
		}
		catch (ResourceAccessException ex) {
			throw new ServiceUnavailableException("customers-service is unreachable while validating the pet");
		}
	}

	private ResourceNotFoundException notFound(Integer ownerId, Integer petId) {
		return new ResourceNotFoundException("Pet " + petId + " of owner", ownerId);
	}

}
