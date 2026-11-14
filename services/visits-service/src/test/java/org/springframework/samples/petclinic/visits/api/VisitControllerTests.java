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
package org.springframework.samples.petclinic.visits.api;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.ProblemDetailExceptionHandler;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.common.web.ServiceUnavailableException;
import org.springframework.samples.petclinic.visits.api.dto.VisitRequest;
import org.springframework.samples.petclinic.visits.api.dto.VisitResponse;
import org.springframework.samples.petclinic.visits.application.VisitService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of the visits API: 201/200/400/404/503 (BR-VIS-01 .. BR-VIS-03).
 */
@WebMvcTest(VisitController.class)
@Import(ProblemDetailExceptionHandler.class)
@DisabledInNativeImage
@DisabledInAotMode
class VisitControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VisitService visits;

	private String tomorrow() {
		return LocalDate.now().plusDays(1).toString();
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		given(this.visits.create(eq(6), eq(7), any(VisitRequest.class)))
			.willReturn(new VisitResponse(1, 6, 7, LocalDate.parse(tomorrow()), "checkup"));

		this.mockMvc.perform(post("/owners/6/pets/7/visits").contentType(MediaType.APPLICATION_JSON).content("""
				{ "date": "%s", "description": "checkup" }
				""".formatted(tomorrow())))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/owners/6/pets/7/visits/1"))
			.andExpect(jsonPath("$.description").value("checkup"));
	}

	@Test
	void BR_VIS_01_descriptionIsMandatoryAndLimitedTo255Characters() throws Exception {
		for (String description : new String[] { "\"  \"", "\"" + "d".repeat(256) + "\"" }) {
			this.mockMvc.perform(post("/owners/6/pets/7/visits").contentType(MediaType.APPLICATION_JSON).content("""
					{ "date": "%s", "description": %s }
					""".formatted(tomorrow(), description)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("description"));
		}
	}

	@Test
	void BR_VIS_02_dateIsMandatory() throws Exception {
		this.mockMvc.perform(post("/owners/6/pets/7/visits").contentType(MediaType.APPLICATION_JSON).content("""
				{ "description": "checkup" }
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("date"));
	}

	@Test
	void BR_VIS_02_aDateFromTodayIsRejected() throws Exception {
		given(this.visits.create(eq(6), eq(7), any(VisitRequest.class)))
			.willThrow(new BadRequestException("date", "must be after today"));

		this.mockMvc.perform(post("/owners/6/pets/7/visits").contentType(MediaType.APPLICATION_JSON).content("""
				{ "date": "%s", "description": "checkup" }
				""".formatted(LocalDate.now())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("date"));
	}

	@Test
	void BR_VIS_03_unknownPetReturns404() throws Exception {
		given(this.visits.create(eq(6), eq(99), any(VisitRequest.class)))
			.willThrow(new ResourceNotFoundException("Pet 99 of owner", 6));

		this.mockMvc.perform(post("/owners/6/pets/99/visits").contentType(MediaType.APPLICATION_JSON).content("""
				{ "date": "%s", "description": "checkup" }
				""".formatted(tomorrow()))).andExpect(status().isNotFound());
	}

	@Test
	void BR_VIS_03_customersServiceDownReturns503() throws Exception {
		given(this.visits.findByPet(6, 7))
			.willThrow(new ServiceUnavailableException("customers-service is unreachable"));

		this.mockMvc.perform(get("/owners/6/pets/7/visits"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.title").value("Service unavailable"))
			.andExpect(jsonPath("$.detail", containsString("unreachable")));
	}

	@Test
	void listReturnsTheVisitsOfThePet() throws Exception {
		given(this.visits.findByPet(6, 7))
			.willReturn(java.util.List.of(new VisitResponse(1, 6, 7, LocalDate.of(2013, 1, 1), "rabies shot")));

		this.mockMvc.perform(get("/owners/6/pets/7/visits"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].description").value("rabies shot"))
			.andExpect(jsonPath("$[0].petId").value(7));
	}

}
