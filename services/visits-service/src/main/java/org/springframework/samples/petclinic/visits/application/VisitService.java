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

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.visits.api.VisitMapper;
import org.springframework.samples.petclinic.visits.api.dto.VisitRequest;
import org.springframework.samples.petclinic.visits.api.dto.VisitResponse;
import org.springframework.samples.petclinic.visits.domain.Visit;
import org.springframework.samples.petclinic.visits.infrastructure.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases around visits.
 */
@Service
@Transactional(readOnly = true)
public class VisitService {

	private final VisitRepository visits;

	private final CustomersGateway customers;

	public VisitService(VisitRepository visits, CustomersGateway customers) {
		this.visits = visits;
		this.customers = customers;
	}

	/**
	 * Visits of a pet that belongs to the owner, ordered by date (BR-PET-09).
	 * @param ownerId the owner from the URL
	 * @param petId the pet from the URL
	 * @return the visits, oldest first
	 */
	public List<VisitResponse> findByPet(Integer ownerId, Integer petId) {
		this.customers.ensurePetBelongsToOwner(ownerId, petId);
		return this.visits.findByOwnerIdAndPetIdOrderByDateAsc(ownerId, petId)
			.stream()
			.map(VisitMapper::toResponse)
			.toList();
	}

	/**
	 * Books a visit. The pet must exist and belong to the owner (BR-VIS-03) and the date
	 * must be strictly in the future (BR-VIS-02).
	 * @param ownerId the owner from the URL
	 * @param petId the pet from the URL
	 * @param request the visit to book
	 * @return the persisted visit
	 */
	@Transactional
	public VisitResponse create(Integer ownerId, Integer petId, VisitRequest request) {
		this.customers.ensurePetBelongsToOwner(ownerId, petId);

		if (!request.date().isAfter(LocalDate.now())) {
			throw new BadRequestException("date", "must be after today");
		}

		Visit visit = new Visit();
		visit.setOwnerId(ownerId);
		visit.setPetId(petId);
		visit.setDate(request.date());
		visit.setDescription(request.description());

		return VisitMapper.toResponse(this.visits.save(visit));
	}

	/**
	 * Visits of several pets in one call, used by the gateway to compose an owner detail.
	 * @param petIds the pets to look up
	 * @return every visit of those pets, ordered by date
	 */
	public List<VisitResponse> findByPets(Collection<Integer> petIds) {
		if (petIds == null || petIds.isEmpty()) {
			throw new BadRequestException("petId", "must not be empty");
		}
		return this.visits.findByPetIdInOrderByDateAsc(petIds).stream().map(VisitMapper::toResponse).toList();
	}

}
