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
package org.springframework.samples.petclinic.customers.infrastructure;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.customers.domain.Owner;
import org.springframework.samples.petclinic.customers.domain.Pet;
import org.springframework.samples.petclinic.customers.domain.PetType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository-level rules checked against the real schema and the legacy seed data
 * (BR-OWN-02, BR-OWN-06, BR-OWN-14, BR-PET-04, BR-PET-08, BR-PET-09, BR-SYS-03).
 */
@DataJpaTest
class CustomersRepositoryTests {

	@Autowired
	private OwnerRepository owners;

	@Autowired
	private PetRepository pets;

	@Autowired
	private PetTypeRepository types;

	@Test
	void BR_SYS_03_seedDataIsTheLegacyOne() {
		assertThat(this.owners.count()).isEqualTo(10);
		assertThat(this.pets.count()).isEqualTo(13);
		assertThat(this.types.count()).isEqualTo(6);
	}

	@Test
	void BR_OWN_02_and_BR_OWN_06_searchByLastNamePrefixIsCaseInsensitive() {
		assertThat(this.owners.findByLastNameStartingWithIgnoreCase("davis", Pageable.unpaged())).hasSize(2);
		assertThat(this.owners.findByLastNameStartingWithIgnoreCase("DAV", Pageable.unpaged())).hasSize(2);
		assertThat(this.owners.findByLastNameStartingWithIgnoreCase("Daviss", Pageable.unpaged())).isEmpty();
		assertThat(this.owners.findByLastNameStartingWithIgnoreCase("", Pageable.unpaged())).hasSize(10);
	}

	@Test
	void BR_OWN_14_pagesAreStableWithTheDocumentedSort() {
		Pageable pageable = PageRequest.of(0, 3, Sort.by("lastName", "firstName", "id"));

		Page<Owner> firstPage = this.owners.findAll(pageable);

		assertThat(firstPage.getContent()).extracting(Owner::getLastName).containsExactly("Black", "Coleman", "Davis");
		assertThat(firstPage.getContent().getFirst().getFirstName()).isEqualTo("Jeff");
	}

	@Test
	void BR_PET_04_theSameNameForTheSameOwnerIsRejectedCaseInsensitively() {
		Owner owner = this.owners.findById(1).orElseThrow();
		Pet duplicate = new Pet();
		duplicate.setName("leo");
		duplicate.setBirthDate(LocalDate.of(2020, 1, 1));
		duplicate.setType(this.types.findById(1).orElseThrow());
		duplicate.setOwner(owner);

		assertThatThrownBy(() -> this.pets.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void BR_PET_04_theSameNameForADifferentOwnerIsAllowed() {
		Owner other = this.owners.findById(2).orElseThrow();
		Pet pet = new Pet();
		pet.setName("leo");
		pet.setBirthDate(LocalDate.of(2020, 1, 1));
		pet.setType(this.types.findById(1).orElseThrow());
		pet.setOwner(other);

		assertThat(this.pets.saveAndFlush(pet).getId()).isNotNull();
	}

	@Test
	void BR_PET_08_petTypesAreOrderedByName() {
		List<PetType> all = this.types.findAllByOrderByNameAsc();

		assertThat(all).extracting(PetType::getName)
			.containsExactly("bird", "cat", "dog", "hamster", "lizard", "snake");
	}

	@Test
	void BR_PET_09_petsOfAnOwnerAreOrderedByName() {
		Owner owner = this.owners.findById(6).orElseThrow();

		assertThat(owner.getPets()).extracting(Pet::getName).containsExactly("Max", "Samantha");
	}

	@Test
	void BR_PET_07_aPetIsOnlyReachableThroughItsOwner() {
		assertThat(this.pets.findByIdAndOwner_Id(1, 1)).isPresent();
		assertThat(this.pets.findByIdAndOwner_Id(1, 2)).isEmpty();
	}

}
