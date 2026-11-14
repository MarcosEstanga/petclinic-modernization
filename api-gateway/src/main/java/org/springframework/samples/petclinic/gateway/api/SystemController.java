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
package org.springframework.samples.petclinic.gateway.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Port of the legacy {@code /oups} endpoint (BR-SYS-01): it always fails, to show that
 * unexpected errors are handled uniformly and, in the legacy application, do not leak a
 * stack trace.
 */
@RestController
@RequestMapping("/api/system")
@Tag(name = "System", description = "Operational endpoints")
public class SystemController {

	@GetMapping("/boom")
	@Operation(summary = "Always fails on purpose",
			description = "Ported from the legacy /oups endpoint to demonstrate error handling (BR-SYS-01).",
			responses = @ApiResponse(responseCode = "500", description = "Expected failure",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))))
	public void boom() {
		throw new IllegalStateException("Expected: /api/system/boom always fails to demonstrate error handling");
	}

}
