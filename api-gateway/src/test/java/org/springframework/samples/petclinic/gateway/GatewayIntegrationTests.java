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
package org.springframework.samples.petclinic.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gateway rules: prefix routing, aggregated composition and error mapping.
 * <p>
 * A tiny stub server (JDK {@link HttpServer}, no extra dependency) plays the three
 * services, so the test verifies the real routing and the real {@code RestClient} calls
 * without starting five JVMs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTests {

	private static final String OWNER_JSON = """
			{
			  "id": 6,
			  "firstName": "Jean",
			  "lastName": "Coleman",
			  "address": "105 N. Lake St.",
			  "city": "Monona",
			  "telephone": "6085552654",
			  "pets": [
			    { "id": 7, "name": "Samantha", "birthDate": "2012-09-04", "type": { "id": 1, "name": "cat" } },
			    { "id": 8, "name": "Max", "birthDate": "2012-09-04", "type": { "id": 1, "name": "cat" } }
			  ]
			}
			""";

	private static final String VISITS_JSON = """
			[
			  { "id": 1, "ownerId": 6, "petId": 7, "date": "2013-01-01", "description": "rabies shot" },
			  { "id": 2, "ownerId": 6, "petId": 8, "date": "2013-01-02", "description": "neutered" }
			]
			""";

	private static final HttpServer STUB = startStub();

	private static final int STUB_PORT = STUB.getAddress().getPort();

	@LocalServerPort
	private int port;

	private RestClient client;

	@DynamicPropertySource
	static void serviceUrls(DynamicPropertyRegistry registry) {
		String baseUrl = "http://localhost:" + STUB_PORT;
		registry.add("customers-service.url", () -> baseUrl);
		registry.add("vets-service.url", () -> baseUrl);
		registry.add("visits-service.url", () -> baseUrl);
	}

	private static HttpServer startStub() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/owners/6", exchange -> respond(exchange, 200, OWNER_JSON));
			server.createContext("/owners/999", exchange -> respond(exchange, 404, "{}"));
			server.createContext("/pets/visits", exchange -> respond(exchange, 200, VISITS_JSON));
			server.createContext("/vets/all", exchange -> respond(exchange, 200,
					"""
							{ "vetList": [ { "id": 1, "firstName": "James", "lastName": "Carter", "specialties": [], "nrOfSpecialties": 0 } ] }
							"""));
			server.start();
			return server;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot start the stub server", ex);
		}
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	@AfterAll
	static void stopStub() {
		STUB.stop(0);
	}

	@BeforeEach
	void setUp() {
		this.client = RestClient.create("http://localhost:" + port);
	}

	@Test
	@SuppressWarnings("unchecked")
	void customersPrefixIsProxiedAndThePrefixIsStripped() {
		ResponseEntity<Map<String, Object>> response = this.client.get()
			.uri("/api/customers/owners/6")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("lastName")).isEqualTo("Coleman");
	}

	@Test
	@SuppressWarnings("unchecked")
	void vetsPrefixIsProxied() {
		ResponseEntity<Map<String, Object>> response = this.client.get()
			.uri("/api/vets/all")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat((List<Map<String, Object>>) response.getBody().get("vetList")).hasSize(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	void ownerDetailsComposesOwnerPetsAndVisits() {
		ResponseEntity<Map<String, Object>> response = this.client.get()
			.uri("/api/owners/6/details")
			.retrieve()
			.toEntity((Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("lastName")).isEqualTo("Coleman");

		List<Map<String, Object>> pets = (List<Map<String, Object>>) response.getBody().get("pets");
		assertThat(pets).extracting(pet -> pet.get("name")).containsExactly("Samantha", "Max");

		List<Map<String, Object>> samanthaVisits = (List<Map<String, Object>>) pets.getFirst().get("visits");
		assertThat(samanthaVisits).extracting(visit -> visit.get("description")).containsExactly("rabies shot");

		List<Map<String, Object>> maxVisits = (List<Map<String, Object>>) pets.get(1).get("visits");
		assertThat(maxVisits).extracting(visit -> visit.get("description")).containsExactly("neutered");
	}

	@Test
	void unknownOwnerIsMappedTo404() {
		assertThatThrownBy(() -> this.client.get().uri("/api/owners/999/details").retrieve().toBodilessEntity())
			.isInstanceOfSatisfying(RestClientResponseException.class, ex -> {
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(ex.getResponseBodyAsString()).contains("Owner with id 999 not found");
			});
	}

	@Test
	void BR_SYS_01_boomEndpointFailsOnPurpose() {
		assertThatThrownBy(() -> this.client.get().uri("/api/system/boom").retrieve().toBodilessEntity())
			.isInstanceOfSatisfying(RestClientResponseException.class,
					ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
	}

	@Test
	void aggregatedSwaggerUiIsPublished() {
		ResponseEntity<String> swagger = this.client.get().uri("/swagger-ui.html").retrieve().toEntity(String.class);

		assertThat(swagger.getStatusCode().is3xxRedirection() || swagger.getStatusCode().is2xxSuccessful()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void gatewayPublishesItsOwnOpenApi() {
		ResponseEntity<Map<String, Object>> apiDocs = this.client.get()
			.uri("/v3/api-docs")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		Map<String, Object> paths = (Map<String, Object>) apiDocs.getBody().get("paths");
		assertThat(paths).containsKey("/api/owners/{ownerId}/details");
	}

}
