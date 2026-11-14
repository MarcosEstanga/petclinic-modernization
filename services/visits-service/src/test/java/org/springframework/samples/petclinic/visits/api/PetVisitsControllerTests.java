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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.common.web.ProblemDetailExceptionHandler;
import org.springframework.samples.petclinic.visits.api.dto.VisitResponse;
import org.springframework.samples.petclinic.visits.application.VisitService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bulk visit lookup used by the gateway composition.
 */
@WebMvcTest(PetVisitsController.class)
@Import(ProblemDetailExceptionHandler.class)
@DisabledInNativeImage
@DisabledInAotMode
class PetVisitsControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VisitService visits;

	@Test
	void commaSeparatedPetIdsAreAccepted() throws Exception {
		given(this.visits.findByPets(List.of(7, 8)))
			.willReturn(List.of(new VisitResponse(1, 6, 7, LocalDate.of(2013, 1, 1), "rabies shot")));

		this.mockMvc.perform(get("/pets/visits").param("petId", "7,8"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].petId").value(7));
	}

}
