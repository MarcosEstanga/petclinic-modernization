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
package org.springframework.samples.petclinic.gateway.application;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.common.web.ServiceUnavailableException;
import org.springframework.samples.petclinic.gateway.api.dto.OwnerDetailsView;
import org.springframework.samples.petclinic.gateway.api.dto.OwnerDetailsView.PetTypeView;
import org.springframework.samples.petclinic.gateway.api.dto.OwnerDetailsView.PetView;
import org.springframework.samples.petclinic.gateway.api.dto.OwnerDetailsView.VisitView;
import org.springframework.samples.petclinic.gateway.infrastructure.client.CustomersClient;
import org.springframework.samples.petclinic.gateway.infrastructure.client.VisitsClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Composes an owner with its pets and visits, which in the monolith was a single
 * Thymeleaf page backed by one JPA aggregate.
 * <p>
 * It performs two calls (owner, then visits of those pets) and translates failures: a
 * missing owner is a 404, a failing dependency a 503 (ADR-005).
 */
@Service
public class OwnerDetailsComposer {

	private final CustomersClient customers;

	private final VisitsClient visits;

	public OwnerDetailsComposer(CustomersClient customers, VisitsClient visits) {
		this.customers = customers;
		this.visits = visits;
	}

	public OwnerDetailsView compose(Integer ownerId) {
		CustomersClient.OwnerDto owner = call("customers-service", () -> this.customers.getOwner(ownerId), ownerId);

		List<Integer> petIds = owner.pets().stream().map(CustomersClient.PetDto::id).toList();
		List<VisitsClient.VisitDto> visitsOfOwner = petIds.isEmpty() ? List.of()
				: call("visits-service", () -> this.visits.findVisits(join(petIds)), ownerId);

		Map<Integer, List<VisitsClient.VisitDto>> byPet = visitsOfOwner.stream()
			.collect(Collectors.groupingBy(VisitsClient.VisitDto::petId));

		List<PetView> pets = owner.pets()
			.stream()
			.map(pet -> new PetView(pet.id(), pet.name(), pet.birthDate(),
					new PetTypeView(pet.type().id(), pet.type().name()),
					byPet.getOrDefault(pet.id(), List.of())
						.stream()
						.map(visit -> new VisitView(visit.id(), visit.date(), visit.description()))
						.toList()))
			.toList();

		return new OwnerDetailsView(owner.id(), owner.firstName(), owner.lastName(), owner.address(), owner.city(),
				owner.telephone(), pets);
	}

	private String join(List<Integer> petIds) {
		return petIds.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	private <T> T call(String service, Supplier<T> call, Integer ownerId) {
		try {
			return call.get();
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
				throw new ResourceNotFoundException("Owner", ownerId);
			}
			throw new ServiceUnavailableException(service + " responded " + ex.getStatusCode());
		}
		catch (ResourceAccessException ex) {
			throw new ServiceUnavailableException(service + " is unreachable");
		}
	}

}
