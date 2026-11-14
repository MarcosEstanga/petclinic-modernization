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

import java.util.List;

import org.springframework.samples.petclinic.customers.api.PetMapper;
import org.springframework.samples.petclinic.customers.api.dto.PetTypeResponse;
import org.springframework.samples.petclinic.customers.infrastructure.PetTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only catalog of pet types, always sorted by name (BR-PET-08).
 */
@Service
@Transactional(readOnly = true)
public class PetTypeService {

	private final PetTypeRepository types;

	public PetTypeService(PetTypeRepository types) {
		this.types = types;
	}

	public List<PetTypeResponse> findAll() {
		return this.types.findAllByOrderByNameAsc().stream().map(PetMapper::toResponse).toList();
	}

}
