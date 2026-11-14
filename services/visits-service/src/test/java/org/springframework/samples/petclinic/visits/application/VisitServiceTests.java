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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.visits.api.dto.VisitRequest;
import org.springframework.samples.petclinic.visits.domain.Visit;
import org.springframework.samples.petclinic.visits.infrastructure.VisitRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Visit rules: future date (BR-VIS-02), default date (BR-VIS-04) and pet/owner validation
 * (BR-VIS-03, BR-VIS-05).
 */
@ExtendWith(MockitoExtension.class)
class VisitServiceTests {

	@Mock
	private VisitRepository visits;

	@Mock
	private CustomersGateway customers;

	@InjectMocks
	private VisitService service;

	@Test
	void BR_VIS_04_aNewVisitDefaultsToTomorrow() {
		Visit visit = new Visit();

		assertThat(visit.getDate()).isEqualTo(LocalDate.now().plusDays(1));
	}

	@Test
	void BR_VIS_02_aDateFromTodayIsRejected() {
		VisitRequest request = new VisitRequest(LocalDate.now(), "checkup");

		assertThatThrownBy(() -> this.service.create(6, 7, request)).isInstanceOf(BadRequestException.class)
			.hasMessageContaining("must be after today");

		verify(this.visits, never()).save(any(Visit.class));
	}

	@Test
	void BR_VIS_02_aDateInThePastIsRejected() {
		VisitRequest request = new VisitRequest(LocalDate.now().minusDays(1), "checkup");

		assertThatThrownBy(() -> this.service.create(6, 7, request)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void BR_VIS_03_and_BR_VIS_05_nothingIsStoredWhenThePetDoesNotBelongToTheOwner() {
		willThrow(new ResourceNotFoundException("Pet 7 of owner", 6)).given(this.customers)
			.ensurePetBelongsToOwner(6, 7);

		VisitRequest request = new VisitRequest(LocalDate.now().plusDays(2), "checkup");

		assertThatThrownBy(() -> this.service.create(6, 7, request)).isInstanceOf(ResourceNotFoundException.class);
		verify(this.visits, never()).save(any(Visit.class));
	}

	@Test
	void BR_VIS_03_theOwnerAndPetAreValidatedBeforeReading() {
		Visit visit = new Visit();
		visit.setId(1);
		visit.setOwnerId(6);
		visit.setPetId(7);
		visit.setDate(LocalDate.of(2013, 1, 1));
		visit.setDescription("rabies shot");
		given(this.visits.findByOwnerIdAndPetIdOrderByDateAsc(6, 7)).willReturn(List.of(visit));

		var result = this.service.findByPet(6, 7);

		verify(this.customers).ensurePetBelongsToOwner(6, 7);
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().description()).isEqualTo("rabies shot");
	}

	@Test
	void createStoresTheVisitScopedByOwnerAndPet() {
		given(this.visits.save(any(Visit.class))).willAnswer(invocation -> {
			Visit saved = invocation.getArgument(0);
			saved.setId(42);
			return saved;
		});

		var result = this.service.create(6, 7, new VisitRequest(LocalDate.now().plusDays(3), "vaccination"));

		assertThat(result.id()).isEqualTo(42);
		assertThat(result.ownerId()).isEqualTo(6);
		assertThat(result.petId()).isEqualTo(7);
		assertThat(result.description()).isEqualTo("vaccination");
	}

	@Test
	void bulkLookupRequiresAtLeastOnePet() {
		assertThatThrownBy(() -> this.service.findByPets(List.of())).isInstanceOf(BadRequestException.class)
			.hasMessageContaining("must not be empty");
	}

	@Test
	void BR_PET_12_aPetThatDoesNotBelongToTheOwnerCannotBeUsedForAVisit() {
		willThrow(new ResourceNotFoundException("Pet 9 of owner", 1)).given(this.customers)
			.ensurePetBelongsToOwner(1, 9);

		assertThatThrownBy(() -> this.service.findByPet(1, 9)).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("Pet 9 of owner with id 1 not found");
	}

}
