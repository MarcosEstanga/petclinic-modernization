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
package org.springframework.samples.petclinic.vets.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.vets.domain.Specialty;
import org.springframework.samples.petclinic.vets.domain.Vet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository rules against the real schema and the legacy seed data (BR-VET-03,
 * BR-SYS-03).
 */
@DataJpaTest
class VetsRepositoryTests {

	@Autowired
	private VetRepository vets;

	@Test
	void BR_SYS_03_seedDataIsTheLegacyOne() {
		assertThat(this.vets.count()).isEqualTo(6);
	}

	@Test
	void BR_VET_03_specialtiesComeOrderedAndCounted() {
		Vet linda = this.vets.findById(3).orElseThrow();

		assertThat(linda.getLastName()).isEqualTo("Douglas");
		assertThat(linda.getSpecialties()).extracting(Specialty::getName).containsExactly("dentistry", "surgery");
		assertThat(linda.getNrOfSpecialties()).isEqualTo(2);
	}

	@Test
	void vetsAreReturnedInTheRequestedOrder() {
		assertThat(this.vets.findAll(Sort.by("lastName"))).extracting(Vet::getLastName)
			.containsExactly("Carter", "Douglas", "Jenkins", "Leary", "Ortega", "Stevens");
	}

}
