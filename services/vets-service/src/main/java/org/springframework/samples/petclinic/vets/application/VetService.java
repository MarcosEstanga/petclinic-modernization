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
package org.springframework.samples.petclinic.vets.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.vets.api.VetMapper;
import org.springframework.samples.petclinic.vets.api.dto.VetResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetsResponse;
import org.springframework.samples.petclinic.vets.domain.Vet;
import org.springframework.samples.petclinic.vets.infrastructure.VetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only use cases for veterinarians. Results are cached in the {@code vets} cache,
 * mirroring the legacy {@code @Cacheable("vets")} repository (BR-VET-04).
 */
@Service
@Transactional(readOnly = true)
public class VetService {

	/** Maximum page size accepted by the API (ADR-004). */
	public static final int MAX_PAGE_SIZE = 100;

	/**
	 * Stable order so pagination is deterministic (the legacy list had no explicit sort).
	 */
	private static final Sort STABLE_SORT = Sort.by("lastName", "firstName", "id");

	private final VetRepository vets;

	public VetService(VetRepository vets) {
		this.vets = vets;
	}

	/**
	 * Paginated list of vets. A page below 1 or beyond the last one is served as page 1
	 * (BR-VET-01, ADR-004).
	 * @param page 1-based page number
	 * @param size page size, 1..100
	 * @return the requested page
	 */
	@Cacheable("vets")
	public PageResponse<VetResponse> findPage(int page, int size) {
		int pageSize = validateSize(size);
		int requestedPage = Math.max(page, 1);

		Page<Vet> result = this.vets.findAll(PageRequest.of(requestedPage - 1, pageSize, STABLE_SORT));
		int servedPage = requestedPage;
		if (result.isEmpty() && requestedPage > 1) {
			servedPage = 1;
			result = this.vets.findAll(PageRequest.of(0, pageSize, STABLE_SORT));
		}
		return PageResponse.of(result.getContent().stream().map(VetMapper::toResponse).toList(), servedPage, pageSize,
				result.getTotalElements());
	}

	/**
	 * Every vet, wrapped in the legacy {@code Vets} object so the JSON root is an object
	 * (BR-VET-02).
	 * @return all vets ordered by last name
	 */
	@Cacheable("vets")
	public VetsResponse findAll() {
		return new VetsResponse(this.vets.findAll(STABLE_SORT).stream().map(VetMapper::toResponse).toList());
	}

	private int validateSize(int size) {
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new BadRequestException("size", "must be between 1 and " + MAX_PAGE_SIZE);
		}
		return size;
	}

}
