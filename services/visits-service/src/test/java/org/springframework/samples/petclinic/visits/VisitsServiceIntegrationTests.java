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
package org.springframework.samples.petclinic.visits;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.common.web.ServiceUnavailableException;
import org.springframework.samples.petclinic.visits.application.CustomersGateway;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;

/**
 * End-to-end test of the visits service. customers-service is replaced by a mock of the
 * gateway port, so the test exercises this service's HTTP + JPA stack without needing
 * another process.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VisitsServiceIntegrationTests {

	private static final ParameterizedTypeReference<List<Map<String, Object>>> VISIT_LIST = new ParameterizedTypeReference<>() {
	};

	@LocalServerPort
	private int port;

	@MockitoBean
	private CustomersGateway customers;

	private RestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestClient.create("http://localhost:" + port);
	}

	@Test
	@SuppressWarnings("unchecked")
	void booksAVisitAndReadsItBackOrderedByDate() {
		ResponseEntity<Map<String, Object>> created = this.client.post()
			.uri("/owners/6/pets/7/visits")
			.body(Map.of("date", LocalDate.now().plusDays(7).toString(), "description", "annual checkup"))
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getHeaders().getLocation()).isNotNull();
		assertThat(created.getBody().get("petId")).isEqualTo(7);

		ResponseEntity<List<Map<String, Object>>> visits = this.client.get()
			.uri("/owners/6/pets/7/visits")
			.retrieve()
			.toEntity(VISIT_LIST);

		assertThat(visits.getBody()).hasSizeGreaterThanOrEqualTo(3);
		assertThat(visits.getBody().getFirst().get("date")).isEqualTo("2013-01-01");
		assertThat(visits.getBody().getLast().get("description")).isEqualTo("annual checkup");
	}

	@Test
	void aPastDateIsRejected() {
		assertThatThrownBy(() -> this.client.post()
			.uri("/owners/6/pets/7/visits")
			.body(Map.of("date", LocalDate.now().toString(), "description", "too late"))
			.retrieve()
			.toBodilessEntity()).isInstanceOfSatisfying(RestClientResponseException.class,
					ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void BR_VIS_03_anUnknownPetIsA404() {
		willThrow(new ResourceNotFoundException("Pet 99 of owner", 6)).given(this.customers)
			.ensurePetBelongsToOwner(6, 99);

		assertThatThrownBy(() -> this.client.get().uri("/owners/6/pets/99/visits").retrieve().toBodilessEntity())
			.isInstanceOfSatisfying(RestClientResponseException.class,
					ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void BR_VIS_03_aDeadCustomersServiceIsA503() {
		willThrow(new ServiceUnavailableException("customers-service is unreachable")).given(this.customers)
			.ensurePetBelongsToOwner(6, 7);

		assertThatThrownBy(() -> this.client.get().uri("/owners/6/pets/7/visits").retrieve().toBodilessEntity())
			.isInstanceOfSatisfying(RestClientResponseException.class, ex -> {
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
				assertThat(ex.getResponseBodyAsString()).contains("unreachable");
			});
	}

	@Test
	void bulkLookupWorksForTheGateway() {
		ResponseEntity<List<Map<String, Object>>> visits = this.client.get()
			.uri("/pets/visits?petId=7,8")
			.retrieve()
			.toEntity(VISIT_LIST);

		assertThat(visits.getBody()).hasSizeGreaterThanOrEqualTo(4);
	}

}
