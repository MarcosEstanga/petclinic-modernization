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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.common.web.ProblemDetailExceptionHandler;
import org.springframework.samples.petclinic.customers.api.dto.PetTypeResponse;
import org.springframework.samples.petclinic.customers.application.PetTypeService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pet types catalog endpoint (BR-PET-08).
 */
@WebMvcTest(PetTypeController.class)
@Import(ProblemDetailExceptionHandler.class)
@DisabledInNativeImage
@DisabledInAotMode
class PetTypeControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PetTypeService types;

	@Test
	void BR_PET_08_typesAreReturnedInTheServiceOrder() throws Exception {
		given(this.types.findAll()).willReturn(List.of(new PetTypeResponse(5, "bird"), new PetTypeResponse(1, "cat")));

		this.mockMvc.perform(get("/petTypes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("bird"))
			.andExpect(jsonPath("$[1].name").value("cat"));
	}

}
