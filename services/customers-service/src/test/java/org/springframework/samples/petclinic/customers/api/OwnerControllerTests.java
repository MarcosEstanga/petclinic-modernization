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
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.common.web.BadRequestException;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.common.web.ProblemDetailExceptionHandler;
import org.springframework.samples.petclinic.common.web.ResourceNotFoundException;
import org.springframework.samples.petclinic.customers.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.customers.api.dto.OwnerRequest;
import org.springframework.samples.petclinic.customers.api.dto.OwnerSummary;
import org.springframework.samples.petclinic.customers.application.OwnerService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of the owners API, including every Bean Validation rule (BR-OWN-01 ..
 * BR-OWN-05) and the identifiers that clients are not allowed to set (BR-OWN-10,
 * BR-OWN-11).
 */
@WebMvcTest(OwnerController.class)
@Import(ProblemDetailExceptionHandler.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerControllerTests {

	private static final String VALID_OWNER = """
			{
			  "firstName": "George",
			  "lastName": "Franklin",
			  "address": "110 W. Liberty St.",
			  "city": "Madison",
			  "telephone": "6085551023"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerService owners;

	private OwnerDetails details() {
		return new OwnerDetails(1, "George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023", List.of());
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		given(this.owners.create(any(OwnerRequest.class))).willReturn(details());

		this.mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content(VALID_OWNER))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/owners/1"))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.firstName").value("George"));
	}

	@Test
	void BR_OWN_01_firstNameIsMandatory() throws Exception {
		String body = VALID_OWNER.replace("\"George\"", "\" \"");

		this.mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors[0].field").value("firstName"));
	}

	@Test
	void BR_OWN_02_lastNameIsLimitedTo30Characters() throws Exception {
		String body = VALID_OWNER.replace("\"Franklin\"", "\"" + "F".repeat(31) + "\"");

		this.mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("lastName"));
	}

	@Test
	void BR_OWN_03_addressIsLimitedTo255Characters() throws Exception {
		String body = VALID_OWNER.replace("\"110 W. Liberty St.\"", "\"" + "A".repeat(256) + "\"");

		this.mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("address"));
	}

	@Test
	void BR_OWN_04_cityIsLimitedTo80Characters() throws Exception {
		String body = VALID_OWNER.replace("\"Madison\"", "\"" + "C".repeat(81) + "\"");

		this.mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("city"));
	}

	@Test
	void BR_OWN_05_telephoneMustHaveExactly10Digits() throws Exception {
		for (String telephone : List.of("\"608555102\"", "\"60855510234\"", "\"608-555-10\"")) {
			String body = VALID_OWNER.replace("\"6085551023\"", telephone);

			this.mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("telephone"))
				.andExpect(jsonPath("$.errors[0].message", containsString("10-digit")));
		}
	}

	@Test
	void BR_OWN_10_and_BR_OWN_11_identifierInTheBodyIsIgnored() throws Exception {
		given(this.owners.create(any(OwnerRequest.class))).willReturn(details());
		String body = VALID_OWNER.replaceFirst("\\{", "{ \"id\": 999,");

		this.mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/owners/1"))
			.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void BR_OWN_09_unknownOwnerReturns404ProblemDetail() throws Exception {
		given(this.owners.findById(999)).willThrow(new ResourceNotFoundException("Owner", 999));

		this.mockMvc.perform(get("/owners/999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Resource not found"))
			.andExpect(jsonPath("$.detail", containsString("Owner with id 999 not found")))
			.andExpect(jsonPath("$.instance").value("/owners/999"));
	}

	@Test
	void searchReturnsPageMetadata() throws Exception {
		given(this.owners.search(eq("Franklin"), eq(1), eq(5))).willReturn(PageResponse.of(
				List.of(new OwnerSummary(1, "George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")), 1, 5,
				1));

		this.mockMvc.perform(get("/owners").param("lastName", "Franklin"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].lastName").value("Franklin"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(5))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(true));
	}

	@Test
	void searchWithAnInvalidSizeReturns400() throws Exception {
		given(this.owners.search(isNull(), eq(1), eq(500)))
			.willThrow(new BadRequestException("size", "must be between 1 and 100"));

		this.mockMvc.perform(get("/owners").param("size", "500"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("size"));
	}

	@Test
	void searchWithANonNumericPageReturns400() throws Exception {
		this.mockMvc.perform(get("/owners").param("page", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid parameter"));
	}

	@Test
	void updateUsesTheIdentifierFromTheUrl() throws Exception {
		given(this.owners.update(eq(7), any(OwnerRequest.class))).willReturn(details());

		this.mockMvc.perform(put("/owners/7").contentType(MediaType.APPLICATION_JSON).content(VALID_OWNER))
			.andExpect(status().isOk());

		verify(this.owners).update(eq(7), any(OwnerRequest.class));
	}

}
