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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.customers.api.OwnerMapper;
import org.springframework.samples.petclinic.customers.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.customers.api.dto.OwnerRequest;
import org.springframework.samples.petclinic.customers.api.dto.OwnerSummary;
import org.springframework.samples.petclinic.customers.domain.Owner;
import org.springframework.samples.petclinic.customers.infrastructure.OwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases around the owner aggregate.
 */
@Service
@Transactional(readOnly = true)
public class OwnerService {

	/** Maximum page size accepted by the API (ADR-004). */
	public static final int MAX_PAGE_SIZE = 100;

	/** Stable order so pagination never repeats or skips rows (BR-OWN-14). */
	private static final Sort STABLE_SORT = Sort.by("lastName", "firstName", "id");

	private final OwnerRepository owners;

	public OwnerService(OwnerRepository owners) {
		this.owners = owners;
	}

	/**
	 * Searches owners by last-name prefix (BR-OWN-06, BR-OWN-07).
	 * <p>
	 * A page below 1 or beyond the last page is served as page 1 instead of redirecting,
	 * and the response tells the client which page it actually received (ADR-004).
	 * @param lastName optional last-name prefix, trimmed; empty or blank means "all
	 * owners"
	 * @param page 1-based page number
	 * @param size page size, between 1 and {@value #MAX_PAGE_SIZE}
	 * @return the requested page
	 */
	public PageResponse<OwnerSummary> search(String lastName, int page, int size) {
		int pageSize = validateSize(size);
		int requestedPage = Math.max(page, 1);
		String prefix = (lastName == null) ? "" : lastName.strip();

		Page<Owner> result = find(prefix, requestedPage, pageSize);
		int servedPage = requestedPage;
		if (result.isEmpty() && requestedPage > 1) {
			servedPage = 1;
			result = find(prefix, servedPage, pageSize);
		}
		return PageResponse.of(result.getContent().stream().map(OwnerMapper::toSummary).toList(), servedPage, pageSize,
				result.getTotalElements());
	}

	public OwnerDetails findById(Integer ownerId) {
		return OwnerMapper.toDetails(loadOwner(ownerId));
	}

	@Transactional
	public OwnerDetails create(OwnerRequest request) {
		Owner owner = new Owner();
		apply(owner, request);
		return OwnerMapper.toDetails(this.owners.save(owner));
	}

	/**
	 * Updates an owner. The identifier always comes from the URL: a client cannot change
	 * it (BR-OWN-10, BR-OWN-11).
	 * @param ownerId the owner to update
	 * @param request the new values
	 * @return the updated owner
	 */
	@Transactional
	public OwnerDetails update(Integer ownerId, OwnerRequest request) {
		Owner owner = loadOwner(ownerId);
		apply(owner, request);
		return OwnerMapper.toDetails(this.owners.save(owner));
	}

	private Owner loadOwner(Integer ownerId) {
		return this.owners.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));
	}

	private void apply(Owner owner, OwnerRequest request) {
		owner.setFirstName(request.firstName());
		owner.setLastName(request.lastName());
		owner.setAddress(request.address());
		owner.setCity(request.city());
		owner.setTelephone(request.telephone());
	}

	private Page<Owner> find(String prefix, int page, int size) {
		Pageable pageable = PageRequest.of(page - 1, size, STABLE_SORT);
		return prefix.isEmpty() ? this.owners.findAll(pageable)
				: this.owners.findByLastNameStartingWithIgnoreCase(prefix, pageable);
	}

	private int validateSize(int size) {
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new BadRequestException("size", "must be between 1 and " + MAX_PAGE_SIZE);
		}
		return size;
	}

}
