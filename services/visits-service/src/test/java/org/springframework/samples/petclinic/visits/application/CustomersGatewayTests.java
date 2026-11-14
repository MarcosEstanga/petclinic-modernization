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
package org.springframework.samples.petclinic.visits.application;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.common.web.ServiceUnavailableException;
import org.springframework.samples.petclinic.visits.infrastructure.client.CustomersClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Inter-service contract of {@link CustomersClient} / {@link CustomersGateway}: 404
 * becomes a 404, any other failure becomes a 503 (BR-VIS-03, ADR-005). Uses
 * {@code MockRestServiceServer} instead of a live customers-service, so the test runs
 * without Docker or ports.
 */
class CustomersGatewayTests {

	private static final String BASE_URL = "http://customers-service";

	private MockRestServiceServer server;

	private CustomersGateway gateway;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		this.server = MockRestServiceServer.bindTo(builder).build();
		CustomersClient client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
			.build()
			.createClient(CustomersClient.class);
		this.gateway = new CustomersGateway(client);
	}

	@Test
	void aPetOfTheOwnerIsAccepted() {
		this.server.expect(requestTo(BASE_URL + "/owners/6/pets/7")).andRespond(withSuccess("""
				{ "id": 7, "name": "Samantha" }
				""", MediaType.APPLICATION_JSON));

		assertThatCode(() -> this.gateway.ensurePetBelongsToOwner(6, 7)).doesNotThrowAnyException();
		this.server.verify();
	}

	@Test
	void BR_VIS_03_a404FromCustomersIsA404() {
		this.server.expect(requestTo(BASE_URL + "/owners/6/pets/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> this.gateway.ensurePetBelongsToOwner(6, 99)).isInstanceOfSatisfying(
				ResourceNotFoundException.class,
				ex -> org.assertj.core.api.Assertions.assertThat(ex.getMessage()).contains("Pet 99 of owner"));
	}

	@Test
	void BR_VIS_03_aServerErrorFromCustomersIsA503() {
		this.server.expect(requestTo(BASE_URL + "/owners/6/pets/7"))
			.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(() -> this.gateway.ensurePetBelongsToOwner(6, 7))
			.isInstanceOf(ServiceUnavailableException.class)
			.hasMessageContaining("500");
	}

	@Test
	void BR_VIS_03_aTimeoutIsA503() {
		this.server.expect(requestTo(BASE_URL + "/owners/6/pets/7"))
			.andRespond(withException(new SocketTimeoutException("read timed out")));

		assertThatThrownBy(() -> this.gateway.ensurePetBelongsToOwner(6, 7))
			.isInstanceOf(ServiceUnavailableException.class)
			.hasMessageContaining("unreachable");
	}

}
