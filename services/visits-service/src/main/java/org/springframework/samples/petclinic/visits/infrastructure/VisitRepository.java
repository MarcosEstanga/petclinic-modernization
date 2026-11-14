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

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.samples.petclinic.visits.domain.Visit;

/**
 * Visits repository. Every query is ordered by date, like the legacy
 * {@code @OrderBy("date ASC")} (BR-PET-09).
 */
public interface VisitRepository extends JpaRepository<Visit, Integer> {

	List<Visit> findByOwnerIdAndPetIdOrderByDateAsc(Integer ownerId, Integer petId);

	List<Visit> findByPetIdInOrderByDateAsc(Collection<Integer> petIds);

}
