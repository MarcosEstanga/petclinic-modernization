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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.customers.api.dto.OwnerSummary;
import org.springframework.samples.petclinic.customers.domain.Owner;
import org.springframework.samples.petclinic.customers.infrastructure.OwnerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Pagination and search rules of the owners API (BR-OWN-06, BR-OWN-07, BR-OWN-12,
 * BR-OWN-14).
 */
@ExtendWith(MockitoExtension.class)
class OwnerServiceTests {

	@Mock
	private OwnerRepository owners;

	@InjectMocks
	private OwnerService service;

	private Owner franklin() {
		Owner owner = new Owner();
		owner.setId(1);
		owner.setFirstName("George");
		owner.setLastName("Franklin");
		owner.setAddress("110 W. Liberty St.");
		owner.setCity("Madison");
		owner.setTelephone("6085551023");
		return owner;
	}

	@Test
	void BR_OWN_06_lastNameIsTrimmedAndUsedAsAPrefix() {
		given(this.owners.findByLastNameStartingWithIgnoreCase(eq("Franklin"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(franklin()), PageRequest.of(0, 5), 1));

		PageResponse<OwnerSummary> result = this.service.search("  Franklin ", 1, 5);

		assertThat(result.content()).hasSize(1);
		verify(this.owners).findByLastNameStartingWithIgnoreCase(eq("Franklin"), any(Pageable.class));
	}

	@Test
	void BR_OWN_06_blankLastNameReturnsEveryOwner() {
		given(this.owners.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(franklin()), PageRequest.of(0, 5), 1));

		PageResponse<OwnerSummary> result = this.service.search("   ", 1, 5);

		assertThat(result.content()).hasSize(1);
		verify(this.owners).findAll(any(Pageable.class));
	}

	@Test
	void BR_OWN_12_pageBelowOneIsClampedInsteadOfBreakingTheQuery() {
		given(this.owners.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(franklin()), PageRequest.of(0, 5), 1));

		PageResponse<OwnerSummary> result = this.service.search("", 0, 5);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(this.owners).findAll(captor.capture());
		assertThat(captor.getValue().getPageNumber()).isZero();
		assertThat(result.page()).isEqualTo(1);
	}

	@Test
	void BR_OWN_08_noMatchesReturnsAnEmptyPageInsteadOfAnError() {
		given(this.owners.findByLastNameStartingWithIgnoreCase(eq("Nobody"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));

		PageResponse<OwnerSummary> result = this.service.search("Nobody", 1, 5);

		assertThat(result.content()).isEmpty();
		assertThat(result.totalElements()).isZero();
		assertThat(result.totalPages()).isZero();
	}

	@Test
	void BR_OWN_07_pageBeyondTheLastPageIsServedAsFirstPage() {
		Pageable requested = PageRequest.of(9, 5);
		given(this.owners.findByLastNameStartingWithIgnoreCase(eq("Davis"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(), requested, 12))
			.willReturn(new PageImpl<>(List.of(franklin()), PageRequest.of(0, 5), 12));

		PageResponse<OwnerSummary> result = this.service.search("Davis", 10, 5);

		assertThat(result.page()).isEqualTo(1);
		assertThat(result.content()).hasSize(1);
		assertThat(result.totalElements()).isEqualTo(12);
	}

	@Test
	void BR_OWN_14_paginationUsesAStableSort() {
		given(this.owners.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(franklin()), PageRequest.of(0, 5), 1));

		this.service.search(null, 1, 5);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(this.owners).findAll(captor.capture());
		assertThat(captor.getValue().getSort().getOrderFor("lastName")).isNotNull();
		assertThat(captor.getValue().getSort().getOrderFor("firstName")).isNotNull();
		assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
	}

	@Test
	void ADR_004_sizeAboveTheLimitIsRejected() {
		assertThatThrownBy(() -> this.service.search("", 1, 101)).isInstanceOf(BadRequestException.class)
			.hasMessageContaining("between 1 and 100");
		assertThatThrownBy(() -> this.service.search("", 1, 0)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void BR_OWN_09_unknownOwnerIsANotFound() {
		given(this.owners.findById(999)).willReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> this.service.findById(999))
			.isInstanceOf(org.springframework.samples.petclinic.common.web.ResourceNotFoundException.class)
			.hasMessageContaining("Owner with id 999 not found");
	}

	@Test
	void searchReturnsPageMetadata() {
		Page<Owner> page = new PageImpl<>(List.of(franklin()), PageRequest.of(0, 5), 12);
		given(this.owners.findByLastNameStartingWithIgnoreCase(anyString(), any(Pageable.class))).willReturn(page);

		PageResponse<OwnerSummary> result = this.service.search("F", 1, 5);

		assertThat(result.totalPages()).isEqualTo(3);
		assertThat(result.first()).isTrue();
		assertThat(result.last()).isFalse();
	}

}
