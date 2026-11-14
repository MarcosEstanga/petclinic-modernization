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
package org.springframework.samples.petclinic.customers;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end test of the customers service over real HTTP, exercising the whole
 * controller → service → repository stack with the H2 schema and the seed data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomersServiceIntegrationTests {

	@LocalServerPort
	private int port;

	private RestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestClient.create("http://localhost:" + port);
	}

	@Test
	@SuppressWarnings("unchecked")
	void createsAnOwnerThenAPetAndReadsThemBack() {
		ResponseEntity<Map<String, Object>> owner = this.client.post()
			.uri("/owners")
			.body(Map.of("firstName", "Ada", "lastName", "Lovelace", "address", "12 Analytical St.", "city", "London",
					"telephone", "6000000001"))
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(owner.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(owner.getHeaders().getLocation()).isNotNull();
		int ownerId = (Integer) owner.getBody().get("id");
		assertThat(owner.getHeaders().getLocation().getPath()).isEqualTo("/owners/" + ownerId);

		ResponseEntity<Map<String, Object>> pet = this.client.post()
			.uri("/owners/{ownerId}/pets", ownerId)
			.body(Map.of("name", "Ada's cat", "birthDate", "2020-02-02", "typeId", 1))
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(pet.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat((Integer) pet.getBody().get("id")).isNotNull();
		assertThat(((Map<String, Object>) pet.getBody().get("type")).get("name")).isEqualTo("cat");

		ResponseEntity<Map<String, Object>> updatedOwner = this.client.get()
			.uri("/owners/{ownerId}", ownerId)
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		List<Map<String, Object>> pets = (List<Map<String, Object>>) updatedOwner.getBody().get("pets");
		assertThat(pets).hasSize(1);
		assertThat(pets.getFirst().get("name")).isEqualTo("Ada's cat");
	}

	@Test
	void duplicatePetNameEndsUpAs409() {
		assertThatThrownBy(() -> this.client.post()
			.uri("/owners/1/pets")
			.body(Map.of("name", "leo", "birthDate", "2020-02-02", "typeId", 1))
			.retrieve()
			.toBodilessEntity()).isInstanceOfSatisfying(RestClientResponseException.class,
					ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void unknownOwnerIsAProblemDetail404() {
		assertThatThrownBy(() -> this.client.get().uri("/owners/999").retrieve().toBodilessEntity())
			.isInstanceOfSatisfying(RestClientResponseException.class, ex -> {
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(ex.getResponseBodyAsString()).contains("Owner with id 999 not found");
			});
	}

	@Test
	void searchFindsFranklinAndReportsPagination() {
		ResponseEntity<Map<String, Object>> response = this.client.get()
			.uri("/owners?lastName=fran&page=1&size=5")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((List<?>) response.getBody().get("content")).hasSize(1);
		assertThat(response.getBody().get("page")).isEqualTo(1);
		assertThat(response.getBody().get("totalElements")).isEqualTo(1);
	}

	@Test
	void outOfRangePageIsServedAsFirstPageWithoutRedirecting() {
		ResponseEntity<Map<String, Object>> response = this.client.get()
			.uri("/owners?page=99&size=5")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("page")).isEqualTo(1);
		assertThat((List<?>) response.getBody().get("content")).isNotEmpty();
	}

	@Test
	void openApiDocumentAndSwaggerUiArePublished() {
		@SuppressWarnings("unchecked")
		Map<String, Object> apiDocs = this.client.get()
			.uri("/v3/api-docs")
			.retrieve()
			.body((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat((String) apiDocs.get("openapi")).startsWith("3.");
		@SuppressWarnings("unchecked")
		Map<String, Object> paths = (Map<String, Object>) apiDocs.get("paths");
		assertThat(paths).containsKeys("/owners", "/petTypes", "/owners/{ownerId}/pets");

		ResponseEntity<String> swagger = this.client.get().uri("/swagger-ui.html").retrieve().toEntity(String.class);
		assertThat(swagger.getStatusCode().is3xxRedirection() || swagger.getStatusCode().is2xxSuccessful()).isTrue();
	}

}
