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
package org.springframework.samples.petclinic.customers.api;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.ConflictException;
import org.springframework.samples.petclinic.common.web.ProblemDetailExceptionHandler;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.customers.api.dto.PetRequest;
import org.springframework.samples.petclinic.customers.api.dto.PetResponse;
import org.springframework.samples.petclinic.customers.api.dto.PetTypeResponse;
import org.springframework.samples.petclinic.customers.application.PetService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of the pets API: 201/200/400/404/409 (BR-PET-01, BR-PET-03, BR-PET-04,
 * BR-PET-07).
 */
@WebMvcTest(PetController.class)
@Import(ProblemDetailExceptionHandler.class)
@DisabledInNativeImage
@DisabledInAotMode
class PetControllerTests {

	private static final String VALID_PET = """
			{
			  "name": "Leo",
			  "birthDate": "2010-09-07",
			  "typeId": 1
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PetService pets;

	private PetResponse response() {
		return new PetResponse(9, "Leo", LocalDate.of(2010, 9, 7), new PetTypeResponse(1, "cat"));
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		given(this.pets.create(eq(1), any(PetRequest.class))).willReturn(response());

		this.mockMvc.perform(post("/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(VALID_PET))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/owners/1/pets/9"))
			.andExpect(jsonPath("$.type.name").value("cat"));
	}

	@Test
	void BR_PET_01_nameIsMandatoryAndLimitedTo30Characters() throws Exception {
		for (String name : new String[] { "\" \"", "\"" + "N".repeat(31) + "\"" }) {
			String body = VALID_PET.replace("\"Leo\"", name);

			this.mockMvc.perform(post("/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("name"));
		}
	}

	@Test
	void BR_PET_03_birthDateIsMandatory() throws Exception {
		String body = VALID_PET.replace("\"birthDate\": \"2010-09-07\",", "");

		this.mockMvc.perform(post("/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("birthDate"));
	}

	@Test
	void BR_PET_03_birthDateInTheFutureIsRejectedByTheService() throws Exception {
		given(this.pets.create(eq(1), any(PetRequest.class)))
			.willThrow(new BadRequestException("birthDate", "must not be in the future"));

		this.mockMvc.perform(post("/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(VALID_PET))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("birthDate"));
	}

	@Test
	void BR_PET_04_duplicateNameReturns409() throws Exception {
		given(this.pets.create(eq(1), any(PetRequest.class)))
			.willThrow(new ConflictException("name", "Pet name 'Leo' is already in use for this owner"));

		this.mockMvc.perform(post("/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(VALID_PET))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void BR_PET_07_unknownOwnerReturns404() throws Exception {
		given(this.pets.create(eq(99), any(PetRequest.class))).willThrow(new ResourceNotFoundException("Owner", 99));

		this.mockMvc.perform(post("/owners/99/pets").contentType(MediaType.APPLICATION_JSON).content(VALID_PET))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail", containsString("Owner with id 99 not found")));
	}

	@Test
	void getReturnsThePet() throws Exception {
		given(this.pets.findById(1, 9)).willReturn(response());

		this.mockMvc.perform(get("/owners/1/pets/9"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(9))
			.andExpect(jsonPath("$.name").value("Leo"));
	}

	@Test
	void updateReturnsTheUpdatedPet() throws Exception {
		given(this.pets.update(eq(1), eq(9), any(PetRequest.class))).willReturn(response());

		this.mockMvc.perform(put("/owners/1/pets/9").contentType(MediaType.APPLICATION_JSON).content(VALID_PET))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(9));
	}

}
