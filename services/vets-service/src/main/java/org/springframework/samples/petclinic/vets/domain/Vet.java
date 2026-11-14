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
package org.springframework.samples.petclinic.vets.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * A veterinarian and its specialties.
 * <p>
 * The specialties are always returned sorted by name (BR-VET-03), so API clients do not
 * need to sort them and the JSON stays deterministic.
 */
@Entity
@Table(name = "vets")
public class Vet extends Person {

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "vet_specialties", joinColumns = @JoinColumn(name = "vet_id"),
			inverseJoinColumns = @JoinColumn(name = "specialty_id"))
	private final List<Specialty> specialties = new ArrayList<>();

	public List<Specialty> getSpecialties() {
		return this.specialties.stream().sorted(Comparator.comparing(NamedEntity::getName)).toList();
	}

	public int getNrOfSpecialties() {
		return this.specialties.size();
	}

	public void addSpecialty(Specialty specialty) {
		this.specialties.add(specialty);
	}

}
