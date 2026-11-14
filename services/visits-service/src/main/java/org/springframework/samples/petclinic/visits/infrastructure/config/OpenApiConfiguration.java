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
package org.springframework.samples.petclinic.visits.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the visits service.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	@Bean
	public OpenAPI visitsOpenApi() {
		return new OpenAPI().info(new Info().title("PetClinic · Visits API")
			.version("1.0.0")
			.description("Visits booked for a pet of an owner. Errors follow RFC 9457; a failing "
					+ "customers-service returns 503.")
			.license(new License().name("Apache License 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
	}

}
