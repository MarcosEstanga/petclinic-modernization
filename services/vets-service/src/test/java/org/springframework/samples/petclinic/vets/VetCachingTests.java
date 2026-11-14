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
package org.springframework.samples.petclinic.vets;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.vets.domain.Specialty;
import org.springframework.samples.petclinic.vets.domain.Vet;
import org.springframework.samples.petclinic.vets.infrastructure.VetRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Cache rule BR-VET-04: repeated reads of the vet list must hit the repository only once.
 * <p>
 * The repository is mocked so the assertion is about the caching layer, not about the
 * database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VetCachingTests {

	@LocalServerPort
	private int port;

	@MockitoBean
	private VetRepository vets;

	private RestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestClient.create("http://localhost:" + port);
	}

	private Vet carter() {
		Vet vet = new Vet();
		vet.setId(1);
		vet.setFirstName("James");
		vet.setLastName("Carter");
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		vet.addSpecialty(radiology);
		return vet;
	}

	@Test
	@SuppressWarnings("unchecked")
	void BR_VET_04_theVetListIsServedFromTheCacheOnTheSecondCall() {
		given(this.vets.findAll(any(org.springframework.data.domain.Sort.class))).willReturn(List.of(carter()));

		ResponseEntity<Map<String, Object>> first = this.client.get()
			.uri("/vets/all")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);
		ResponseEntity<Map<String, Object>> second = this.client.get()
			.uri("/vets/all")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(second.getBody()).isEqualTo(first.getBody());
		verify(this.vets, times(1)).findAll(any(org.springframework.data.domain.Sort.class));
	}

}
