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
package org.springframework.samples.petclinic.gateway.infrastructure.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative client for the visits-service bulk lookup.
 */
@HttpExchange("/pets/visits")
public interface VisitsClient {

	/**
	 * Visits of several pets in a single call.
	 * @param petIds comma separated pet ids (for example {@code 7,8})
	 * @return the visits, ordered by date
	 */
	@GetExchange
	List<VisitDto> findVisits(@RequestParam("petId") String petIds);

	/**
	 * Visit as returned by visits-service.
	 *
	 * @param id visit id
	 * @param ownerId owner id
	 * @param petId pet id
	 * @param date visit date
	 * @param description what happened
	 */
	record VisitDto(Integer id, Integer ownerId, Integer petId, LocalDate date, String description) {
	}

}
