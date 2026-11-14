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

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.samples.petclinic.customers.domain.Pet;

/**
 * Pets repository. All lookups are scoped by owner, so a pet can never be reached through
 * the wrong owner (BR-PET-07).
 */
public interface PetRepository extends JpaRepository<Pet, Integer> {

	Optional<Pet> findByIdAndOwner_Id(Integer id, Integer ownerId);

	boolean existsByOwner_IdAndNameIgnoreCase(Integer ownerId, String name);

	Optional<Pet> findByOwner_IdAndNameIgnoreCase(Integer ownerId, String name);

}
