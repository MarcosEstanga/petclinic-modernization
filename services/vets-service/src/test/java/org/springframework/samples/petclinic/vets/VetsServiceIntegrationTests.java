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
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of the vets service over real HTTP with the legacy seed data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VetsServiceIntegrationTests {

	@LocalServerPort
	private int port;

	private RestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestClient.create("http://localhost:" + port);
	}

	@Test
	@SuppressWarnings("unchecked")
	void allVetsAreReturnedInTheLegacyShape() {
		ResponseEntity<Map<String, Object>> response = this.client.get()
			.uri("/vets/all")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> vetList = (List<Map<String, Object>>) response.getBody().get("vetList");
		assertThat(vetList).hasSize(6);
		assertThat(vetList.getFirst().get("lastName")).isEqualTo("Carter");

		Map<String, Object> douglas = vetList.stream()
			.filter(vet -> "Douglas".equals(vet.get("lastName")))
			.findFirst()
			.orElseThrow();
		assertThat(douglas.get("nrOfSpecialties")).isEqualTo(2);
		List<Map<String, Object>> specialties = (List<Map<String, Object>>) douglas.get("specialties");
		assertThat(specialties).extracting(specialty -> specialty.get("name")).containsExactly("dentistry", "surgery");
	}

	@Test
	@SuppressWarnings("unchecked")
	void paginatedListUsesSizeFiveByDefault() {
		ResponseEntity<Map<String, Object>> response = this.client.get()
			.uri("/vets")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(response.getBody().get("size")).isEqualTo(5);
		assertThat((List<?>) response.getBody().get("content")).hasSize(5);
		assertThat(response.getBody().get("totalPages")).isEqualTo(2);
	}

	@Test
	void openApiDocumentIsPublished() {
		@SuppressWarnings("unchecked")
		Map<String, Object> apiDocs = this.client.get()
			.uri("/v3/api-docs")
			.retrieve()
			.body((Class<Map<String, Object>>) (Class<?>) Map.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> paths = (Map<String, Object>) apiDocs.get("paths");
		assertThat(paths).containsKeys("/vets", "/vets/all");
	}

}
