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
package org.springframework.samples.petclinic.customers.application;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.ConflictException;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.customers.api.dto.PetRequest;
import org.springframework.samples.petclinic.customers.domain.Owner;
import org.springframework.samples.petclinic.customers.domain.Pet;
import org.springframework.samples.petclinic.customers.domain.PetType;
import org.springframework.samples.petclinic.customers.infrastructure.OwnerRepository;
import org.springframework.samples.petclinic.customers.infrastructure.PetRepository;
import org.springframework.samples.petclinic.customers.infrastructure.PetTypeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Business rules of the pets API (BR-PET-02 .. BR-PET-07, BR-PET-10).
 */
@ExtendWith(MockitoExtension.class)
class PetServiceTests {

	@Mock
	private PetRepository pets;

	@Mock
	private OwnerRepository owners;

	@Mock
	private PetTypeRepository types;

	@InjectMocks
	private PetService service;

	private Owner owner() {
		Owner owner = new Owner();
		owner.setId(1);
		owner.setFirstName("George");
		owner.setLastName("Franklin");
		owner.setAddress("110 W. Liberty St.");
		owner.setCity("Madison");
		owner.setTelephone("6085551023");
		return owner;
	}

	private PetType cat() {
		PetType type = new PetType();
		type.setId(1);
		type.setName("cat");
		return type;
	}

	private Pet pet(Integer id, String name) {
		Pet pet = new Pet();
		pet.setId(id);
		pet.setName(name);
		pet.setBirthDate(LocalDate.of(2010, 9, 7));
		pet.setType(cat());
		return pet;
	}

	@Test
	void BR_PET_02_typeIsRequiredWhenCreating() {
		given(this.owners.findById(1)).willReturn(Optional.of(owner()));

		assertThatThrownBy(() -> this.service.create(1, new PetRequest("Leo", LocalDate.of(2010, 9, 7), null)))
			.isInstanceOf(BadRequestException.class)
			.hasMessage("is required");

		verify(this.pets, never()).saveAndFlush(any(Pet.class));
	}

	@Test
	void BR_PET_10_unknownPetTypeIsAClientError() {
		given(this.owners.findById(1)).willReturn(Optional.of(owner()));
		given(this.types.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.create(1, new PetRequest("Leo", LocalDate.of(2010, 9, 7), 99)))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("unknown pet type");
	}

	@Test
	void BR_PET_03_birthDateInTheFutureIsRejected() {
		given(this.owners.findById(1)).willReturn(Optional.of(owner()));
		given(this.types.findById(1)).willReturn(Optional.of(cat()));

		PetRequest request = new PetRequest("Leo", LocalDate.now().plusDays(1), 1);

		assertThatThrownBy(() -> this.service.create(1, request)).isInstanceOf(BadRequestException.class)
			.hasMessageContaining("must not be in the future");
	}

	@Test
	void BR_PET_04_duplicateNameForTheSameOwnerIsAConflict() {
		given(this.owners.findById(1)).willReturn(Optional.of(owner()));
		given(this.types.findById(1)).willReturn(Optional.of(cat()));
		given(this.pets.existsByOwner_IdAndNameIgnoreCase(1, "leo")).willReturn(true);

		PetRequest request = new PetRequest("leo", LocalDate.of(2010, 9, 7), 1);

		assertThatThrownBy(() -> this.service.create(1, request)).isInstanceOf(ConflictException.class)
			.hasMessageContaining("already in use");
	}

	@Test
	void BR_PET_05_reusingItsOwnNameIsAllowed() {
		Pet existing = pet(1, "Leo");
		given(this.pets.findByIdAndOwner_Id(1, 1)).willReturn(Optional.of(existing));
		given(this.pets.findByOwner_IdAndNameIgnoreCase(1, "Leo")).willReturn(Optional.of(existing));
		given(this.pets.saveAndFlush(any(Pet.class))).willAnswer(invocation -> invocation.getArgument(0));

		var result = this.service.update(1, 1, new PetRequest("Leo", LocalDate.of(2011, 1, 1), null));

		assertThat(result.name()).isEqualTo("Leo");
		assertThat(result.birthDate()).isEqualTo(LocalDate.of(2011, 1, 1));
	}

	@Test
	void BR_PET_05_renamingToAnotherPetsNameIsAConflict() {
		Pet existing = pet(1, "Leo");
		Pet other = pet(2, "Milo");
		given(this.pets.findByIdAndOwner_Id(1, 1)).willReturn(Optional.of(existing));
		given(this.pets.findByOwner_IdAndNameIgnoreCase(1, "Milo")).willReturn(Optional.of(other));

		PetRequest request = new PetRequest("Milo", LocalDate.of(2010, 9, 7), null);

		assertThatThrownBy(() -> this.service.update(1, 1, request)).isInstanceOf(ConflictException.class);
	}

	@Test
	void BR_PET_06_databaseUniquenessViolationBecomesAConflict() {
		given(this.owners.findById(1)).willReturn(Optional.of(owner()));
		given(this.types.findById(1)).willReturn(Optional.of(cat()));
		given(this.pets.existsByOwner_IdAndNameIgnoreCase(1, "Leo")).willReturn(false);
		given(this.pets.saveAndFlush(any(Pet.class))).willThrow(
				new DataIntegrityViolationException("Unique index or primary key violation: unique_owner_pet_name"));

		PetRequest request = new PetRequest("Leo", LocalDate.of(2010, 9, 7), 1);

		assertThatThrownBy(() -> this.service.create(1, request)).isInstanceOf(ConflictException.class)
			.hasMessageContaining("already in use");
	}

	@Test
	void BR_PET_06_otherIntegrityViolationsArePropagated() {
		given(this.owners.findById(1)).willReturn(Optional.of(owner()));
		given(this.types.findById(1)).willReturn(Optional.of(cat()));
		given(this.pets.existsByOwner_IdAndNameIgnoreCase(1, "Leo")).willReturn(false);
		given(this.pets.saveAndFlush(any(Pet.class)))
			.willThrow(new DataIntegrityViolationException("NOT NULL constraint violated: TYPE_ID"));

		PetRequest request = new PetRequest("Leo", LocalDate.of(2010, 9, 7), 1);

		assertThatThrownBy(() -> this.service.create(1, request)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void BR_PET_07_petOfAnotherOwnerIsANotFound() {
		given(this.pets.findByIdAndOwner_Id(7, 2)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.findById(2, 7)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createPersistsThePetWithTheRequestedType() {
		Owner owner = owner();
		given(this.owners.findById(1)).willReturn(Optional.of(owner));
		given(this.types.findById(1)).willReturn(Optional.of(cat()));
		given(this.pets.saveAndFlush(any(Pet.class))).willAnswer(invocation -> {
			Pet saved = invocation.getArgument(0);
			saved.setId(99);
			return saved;
		});

		var result = this.service.create(1, new PetRequest("Leo", LocalDate.of(2010, 9, 7), 1));

		assertThat(result.id()).isEqualTo(99);
		assertThat(result.type().name()).isEqualTo("cat");
		assertThat(owner.getPets()).hasSize(1);
	}

}
