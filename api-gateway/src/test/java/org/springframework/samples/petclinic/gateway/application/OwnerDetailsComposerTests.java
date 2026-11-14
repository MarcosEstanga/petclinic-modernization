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
package org.springframework.samples.petclinic.gateway.application;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.common.web.ServiceUnavailableException;
import org.springframework.samples.petclinic.gateway.infrastructure.client.CustomersClient;
import org.springframework.samples.petclinic.gateway.infrastructure.client.VisitsClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Composition rules: nesting, empty pets and error translation (ADR-005).
 */
@ExtendWith(MockitoExtension.class)
class OwnerDetailsComposerTests {

	@Mock
	private CustomersClient customers;

	@Mock
	private VisitsClient visits;

	@InjectMocks
	private OwnerDetailsComposer composer;

	private CustomersClient.OwnerDto owner(List<CustomersClient.PetDto> pets) {
		return new CustomersClient.OwnerDto(6, "Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654", pets);
	}

	@Test
	void nestsEachPetsVisits() {
		CustomersClient.PetDto samantha = new CustomersClient.PetDto(7, "Samantha", LocalDate.of(2012, 9, 4),
				new CustomersClient.PetTypeDto(1, "cat"));
		CustomersClient.PetDto max = new CustomersClient.PetDto(8, "Max", LocalDate.of(2012, 9, 4),
				new CustomersClient.PetTypeDto(1, "cat"));
		given(this.customers.getOwner(6)).willReturn(owner(List.of(samantha, max)));
		given(this.visits.findVisits("7,8"))
			.willReturn(List.of(new VisitsClient.VisitDto(1, 6, 7, LocalDate.of(2013, 1, 1), "rabies shot"),
					new VisitsClient.VisitDto(2, 6, 8, LocalDate.of(2013, 1, 2), "neutered")));

		var result = this.composer.compose(6);

		assertThat(result.pets()).hasSize(2);
		assertThat(result.pets().getFirst().visits()).extracting("description").containsExactly("rabies shot");
		assertThat(result.pets().get(1).visits()).extracting("description").containsExactly("neutered");
	}

	@Test
	void anOwnerWithoutPetsDoesNotCallTheVisitsService() {
		given(this.customers.getOwner(6)).willReturn(owner(List.of()));

		var result = this.composer.compose(6);

		assertThat(result.pets()).isEmpty();
		verify(this.visits, never()).findVisits(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void a404FromCustomersIsA404() {
		given(this.customers.getOwner(999)).willThrow(new RestClientResponseException("not found",
				HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null));

		assertThatThrownBy(() -> this.composer.compose(999)).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("Owner with id 999 not found");
	}

	@Test
	void aFailingDependencyIsA503() {
		given(this.customers.getOwner(6)).willReturn(owner(List.of(new CustomersClient.PetDto(7, "Samantha",
				LocalDate.of(2012, 9, 4), new CustomersClient.PetTypeDto(1, "cat")))));
		given(this.visits.findVisits("7"))
			.willThrow(new ResourceAccessException("timed out", new SocketTimeoutException("read timed out")));

		assertThatThrownBy(() -> this.composer.compose(6)).isInstanceOf(ServiceUnavailableException.class)
			.hasMessageContaining("visits-service");
	}

	@Test
	void anUnexpectedRemoteErrorIsA503() {
		given(this.customers.getOwner(6)).willThrow(new RestClientResponseException("boom",
				HttpStatusCode.valueOf(500).value(), "Internal Server Error", null, null, null));

		assertThatThrownBy(() -> this.composer.compose(6)).isInstanceOf(ServiceUnavailableException.class)
			.hasMessageContaining("customers-service");
	}

}
