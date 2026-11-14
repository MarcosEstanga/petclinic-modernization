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
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.ConflictException;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.customers.api.PetMapper;
import org.springframework.samples.petclinic.customers.api.dto.PetRequest;
import org.springframework.samples.petclinic.customers.api.dto.PetResponse;
import org.springframework.samples.petclinic.customers.domain.Owner;
import org.springframework.samples.petclinic.customers.domain.Pet;
import org.springframework.samples.petclinic.customers.domain.PetType;
import org.springframework.samples.petclinic.customers.infrastructure.OwnerRepository;
import org.springframework.samples.petclinic.customers.infrastructure.PetRepository;
import org.springframework.samples.petclinic.customers.infrastructure.PetTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases around pets. Every operation is scoped by owner, replicating the original
 * {@code /owners/{ownerId}/pets/...} routes (BR-PET-07).
 */
@Service
@Transactional
public class PetService {

	/** Name of the database constraint that guards BR-PET-04 under concurrency. */
	static final String DUPLICATE_NAME_CONSTRAINT = "unique_owner_pet_name";

	private final PetRepository pets;

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	public PetService(PetRepository pets, OwnerRepository owners, PetTypeRepository types) {
		this.pets = pets;
		this.owners = owners;
		this.types = types;
	}

	@Transactional(readOnly = true)
	public PetResponse findById(Integer ownerId, Integer petId) {
		return PetMapper.toResponse(loadPet(ownerId, petId));
	}

	/**
	 * Creates a pet for an owner.
	 * @param ownerId the owner from the URL
	 * @param request the new pet
	 * @return the persisted pet
	 */
	public PetResponse create(Integer ownerId, PetRequest request) {
		Owner owner = this.owners.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));

		// BR-PET-02: type is required when creating a new pet
		if (request.typeId() == null) {
			throw new BadRequestException("typeId", "is required");
		}
		PetType type = loadType(request.typeId());
		validateBirthDate(request.birthDate());

		// BR-PET-04: name is unique per owner, case-insensitive
		if (this.pets.existsByOwner_IdAndNameIgnoreCase(ownerId, request.name())) {
			throw duplicateName(request.name());
		}

		Pet pet = new Pet();
		pet.setName(request.name());
		pet.setBirthDate(request.birthDate());
		pet.setType(type);
		owner.addPet(pet);

		return PetMapper.toResponse(saveTranslatingDuplicates(pet));
	}

	/**
	 * Updates a pet. The type may be omitted, in which case the current one is kept
	 * (BR-PET-02); reusing the very same name is allowed (BR-PET-05).
	 * @param ownerId the owner from the URL
	 * @param petId the pet from the URL
	 * @param request the new values
	 * @return the updated pet
	 */
	public PetResponse update(Integer ownerId, Integer petId, PetRequest request) {
		Pet pet = loadPet(ownerId, petId);

		if (request.typeId() != null) {
			pet.setType(loadType(request.typeId()));
		}
		validateBirthDate(request.birthDate());

		// BR-PET-05: a name already used by a *different* pet of the same owner is
		// rejected
		this.pets.findByOwner_IdAndNameIgnoreCase(ownerId, request.name())
			.filter(existing -> !Objects.equals(existing.getId(), petId))
			.ifPresent(existing -> {
				throw duplicateName(request.name());
			});

		pet.setName(request.name());
		pet.setBirthDate(request.birthDate());
		return PetMapper.toResponse(saveTranslatingDuplicates(pet));
	}

	private Pet loadPet(Integer ownerId, Integer petId) {
		return this.pets.findByIdAndOwner_Id(petId, ownerId)
			.orElseThrow(() -> new ResourceNotFoundException("Pet " + petId + " of owner", ownerId));
	}

	private PetType loadType(Integer typeId) {
		// BR-PET-10: an unknown pet type is a client error, never an invented type
		return this.types.findById(typeId)
			.orElseThrow(() -> new BadRequestException("typeId", "unknown pet type: " + typeId));
	}

	private void validateBirthDate(LocalDate birthDate) {
		// BR-PET-03: required (Bean Validation) and never in the future
		if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
			throw new BadRequestException("birthDate", "must not be in the future");
		}
	}

	private Pet saveTranslatingDuplicates(Pet pet) {
		try {
			return this.pets.saveAndFlush(pet);
		}
		catch (DataIntegrityViolationException ex) {
			// BR-PET-06: only the duplicate-name constraint becomes a 409, other
			// violations bubble up
			if (isDuplicateNameViolation(ex)) {
				throw duplicateName(pet.getName());
			}
			throw ex;
		}
	}

	private boolean isDuplicateNameViolation(DataIntegrityViolationException ex) {
		String message = ex.getMessage();
		return message != null && message.toLowerCase().contains(DUPLICATE_NAME_CONSTRAINT);
	}

	private ConflictException duplicateName(String name) {
		return new ConflictException("name", "Pet name '" + name + "' is already in use for this owner");
	}

}
