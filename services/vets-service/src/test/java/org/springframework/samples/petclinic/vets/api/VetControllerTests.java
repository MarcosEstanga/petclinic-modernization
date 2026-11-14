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
package org.springframework.samples.petclinic.vets.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.common.web.ProblemDetailExceptionHandler;
import org.springframework.samples.petclinic.vets.api.dto.SpecialtyResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetResponse;
import org.springframework.samples.petclinic.vets.api.dto.VetsResponse;
import org.springframework.samples.petclinic.vets.application.VetService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of the vets API (BR-VET-01, BR-VET-02, BR-VET-03).
 */
@WebMvcTest(VetController.class)
@Import(ProblemDetailExceptionHandler.class)
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VetService vets;

	private VetResponse helen() {
		return new VetResponse(2, "Helen", "Leary", List.of(new SpecialtyResponse(1, "radiology")), 1);
	}

	@Test
	void BR_VET_02_fullListIsWrappedInVetList() throws Exception {
		given(this.vets.findAll()).willReturn(new VetsResponse(List.of(helen())));

		this.mockMvc.perform(get("/vets/all"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.vetList").isArray())
			.andExpect(jsonPath("$.vetList[0].lastName").value("Leary"))
			.andExpect(jsonPath("$.vetList[0].nrOfSpecialties").value(1))
			.andExpect(jsonPath("$.vetList[0].specialties[0].name").value("radiology"));
	}

	@Test
	void BR_VET_01_paginatedListIncludesMetadata() throws Exception {
		given(this.vets.findPage(1, 5)).willReturn(PageResponse.of(List.of(helen()), 1, 5, 6));

		this.mockMvc.perform(get("/vets").param("page", "1").param("size", "5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].firstName").value("Helen"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalElements").value(6))
			.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void invalidSizeReturnsAProblemDetail() throws Exception {
		given(this.vets.findPage(1, 500)).willThrow(new BadRequestException("size", "must be between 1 and 100"));

		this.mockMvc.perform(get("/vets").param("size", "500"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("size"));
	}

}
