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
package org.springframework.samples.petclinic.customers.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * An owner of pets. Keeps the aggregate the original application had: an owner owns its
 * pets and {@link #getPets()} is always ordered by pet name (BR-PET-09).
 */
@Entity
@Table(name = "owners")
public class Owner extends Person {

	@Column(nullable = false, length = 255)
	@NotBlank
	@Size(max = 255)
	private String address;

	@Column(nullable = false, length = 80)
	@NotBlank
	@Size(max = 80)
	private String city;

	@Column(nullable = false, length = 20)
	@NotBlank
	@Pattern(regexp = "\\d{10}", message = "{telephone.invalid}")
	private String telephone;

	@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderBy("name ASC")
	private final List<Pet> pets = new ArrayList<>();

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return this.city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getTelephone() {
		return this.telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public List<Pet> getPets() {
		return this.pets;
	}

	/**
	 * Adds a pet and keeps both sides of the association consistent.
	 * @param pet the pet to add, must not be {@literal null}
	 */
	public void addPet(Pet pet) {
		pet.setOwner(this);
		this.pets.add(pet);
	}

	/**
	 * Finds a pet of this owner by id, case-insensitively by name when no id is given.
	 * @param id the pet identifier
	 * @return the pet, or empty when it does not belong to this owner
	 */
	public Optional<Pet> findPet(Integer id) {
		return this.pets.stream().filter(pet -> pet.getId() != null && pet.getId().equals(id)).findFirst();
	}

}
