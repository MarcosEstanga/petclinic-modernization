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
package org.springframework.samples.petclinic.gateway.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

/**
 * Routes of the API gateway, written with the Spring Cloud Gateway Server MVC Java DSL.
 * <p>
 * Each route owns a URL prefix ({@code /api/<service>}) and strips it before forwarding,
 * so the downstream services do not need to know how they are exposed (ADR-002).
 * <p>
 * The vets service already publishes its resources under {@code /vets}, so its route
 * strips only {@code /api}; the other two strip {@code /api/<service>}.
 */
@Configuration(proxyBeanMethods = false)
public class GatewayRoutesConfiguration {

	@Bean
	public RouterFunction<ServerResponse> customersRoute(
			@Value("${customers-service.url}") String customersServiceUrl) {
		return route("customers").route(path("/api/customers/**"), http())
			.before(stripPrefix(2))
			.before(uri(customersServiceUrl))
			.build();
	}

	@Bean
	public RouterFunction<ServerResponse> vetsRoute(@Value("${vets-service.url}") String vetsServiceUrl) {
		// The vets endpoints are already published under /vets, so only /api is stripped:
		// /api/vets/all -> /vets/all
		return route("vets").route(path("/api/vets/**"), http())
			.before(stripPrefix(1))
			.before(uri(vetsServiceUrl))
			.build();
	}

	@Bean
	public RouterFunction<ServerResponse> visitsRoute(@Value("${visits-service.url}") String visitsServiceUrl) {
		return route("visits").route(path("/api/visits/**"), http())
			.before(stripPrefix(2))
			.before(uri(visitsServiceUrl))
			.build();
	}

}
