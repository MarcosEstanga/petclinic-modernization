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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetsResponse;
import org.springframework.samples.petclinic.vets.domain.Specialty;
import org.springframework.samples.petclinic.vets.domain.Vet;
import org.springframework.samples.petclinic.vets.infrastructure.VetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Vets rules: pagination (BR-VET-01), wrapper (BR-VET-02) and specialty ordering
 * (BR-VET-03).
 */
@ExtendWith(MockitoExtension.class)
class VetServiceTests {

	@Mock
	private VetRepository vets;

	@InjectMocks
	private VetService service;

	private Vet douglas() {
		Vet vet = new Vet();
		vet.setId(3);
		vet.setFirstName("Linda");
		vet.setLastName("Douglas");
		Specialty surgery = new Specialty();
		surgery.setId(2);
		surgery.setName("surgery");
		Specialty dentistry = new Specialty();
		dentistry.setId(3);
		dentistry.setName("dentistry");
		vet.addSpecialty(surgery);
		vet.addSpecialty(dentistry);
		return vet;
	}

	@Test
	void BR_VET_03_specialtiesAreSortedByNameAndCounted() {
		Vet vet = douglas();

		assertThat(vet.getSpecialties()).extracting(Specialty::getName).containsExactly("dentistry", "surgery");
		assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
	}

	@Test
	void BR_VET_01_aPageBeyondTheEndIsServedAsFirstPage() {
		given(this.vets.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(), PageRequest.of(9, 5), 6))
			.willReturn(new PageImpl<>(List.of(douglas()), PageRequest.of(0, 5), 6));

		PageResponse<VetResponse> page = this.service.findPage(10, 5);

		assertThat(page.page()).isEqualTo(1);
		assertThat(page.content()).hasSize(1);
		assertThat(page.totalElements()).isEqualTo(6);
	}

	@Test
	void BR_VET_01_pageBelowOneIsClampedAndSortedStably() {
		given(this.vets.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(douglas()), PageRequest.of(0, 5), 6));

		this.service.findPage(0, 5);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(this.vets).findAll(captor.capture());
		Sort sort = captor.getValue().getSort();
		assertThat(captor.getValue().getPageNumber()).isZero();
		assertThat(sort.getOrderFor("lastName")).isNotNull();
		assertThat(sort.getOrderFor("id")).isNotNull();
	}

	@Test
	void BR_VET_02_fullListIsWrappedInAVetsObject() {
		given(this.vets.findAll(any(Sort.class))).willReturn(List.of(douglas()));

		VetsResponse response = this.service.findAll();

		assertThat(response.vetList()).hasSize(1);
		assertThat(response.vetList().getFirst().nrOfSpecialties()).isEqualTo(2);
		assertThat(response.vetList().getFirst().specialties()).extracting("name")
			.containsExactly("dentistry", "surgery");
	}

	@Test
	void ADR_004_anInvalidPageSizeIsRejected() {
		assertThatThrownBy(() -> this.service.findPage(1, 0)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> this.service.findPage(1, 101)).isInstanceOf(BadRequestException.class)
			.hasMessageContaining("between 1 and 100");
	}

}
