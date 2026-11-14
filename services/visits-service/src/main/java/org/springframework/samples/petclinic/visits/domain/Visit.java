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
package org.springframework.samples.petclinic.visits.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A visit booked for a pet.
 * <p>
 * The owner id is stored next to the pet id even though this service does not own those
 * entities: it is what customers-service validated when the visit was created, and it
 * keeps visit queries scoped by owner (BR-VIS-03).
 */
@Entity
@Table(name = "visits")
public class Visit extends BaseEntity {

	@Column(name = "owner_id", nullable = false)
	private Integer ownerId;

	@Column(name = "pet_id", nullable = false)
	private Integer petId;

	@Column(name = "visit_date", nullable = false)
	private LocalDate date;

	@Column(nullable = false, length = 255)
	@NotBlank
	@Size(max = 255)
	private String description;

	/**
	 * Creates a visit for tomorrow, preserving the legacy default (BR-VIS-04).
	 */
	public Visit() {
		this.date = LocalDate.now().plusDays(1);
	}

	public Integer getOwnerId() {
		return this.ownerId;
	}

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
	}

	public Integer getPetId() {
		return this.petId;
	}

	public void setPetId(Integer petId) {
		this.petId = petId;
	}

	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
