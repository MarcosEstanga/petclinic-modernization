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
package org.springframework.samples.petclinic.visits.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.samples.petclinic.visits.domain.Visit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visits repository rules against the real schema and the legacy seed data (BR-PET-09,
 * BR-SYS-03).
 */
@DataJpaTest
class VisitRepositoryTests {

	@Autowired
	private VisitRepository visits;

	@Test
	void BR_SYS_03_seedDataIsTheLegacyOne() {
		assertThat(this.visits.count()).isEqualTo(4);
	}

	@Test
	void BR_PET_09_visitsAreOrderedByDate() {
		List<Visit> samantha = this.visits.findByOwnerIdAndPetIdOrderByDateAsc(6, 7);

		assertThat(samantha).extracting(Visit::getDescription).containsExactly("rabies shot", "spayed");
		assertThat(samantha).extracting(Visit::getDate)
			.containsExactly(java.time.LocalDate.of(2013, 1, 1), java.time.LocalDate.of(2013, 1, 4));
	}

	@Test
	void visitsAreScopedByOwnerAndPet() {
		assertThat(this.visits.findByOwnerIdAndPetIdOrderByDateAsc(7, 7)).isEmpty();
	}

	@Test
	void bulkLookupReturnsEveryVisitOfThePets() {
		assertThat(this.visits.findByPetIdInOrderByDateAsc(List.of(7, 8))).hasSize(4);
		assertThat(this.visits.findByPetIdInOrderByDateAsc(List.of(7))).hasSize(2);
	}

}
