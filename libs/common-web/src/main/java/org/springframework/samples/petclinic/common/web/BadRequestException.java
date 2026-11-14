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
package org.springframework.samples.petclinic.common.web;

/**
 * Signals a client error that is not a Bean Validation failure (for example a pet type
 * that does not exist, or a birth date in the future). Mapped to HTTP 400 by
 * {@link ProblemDetailExceptionHandler}.
 * <p>
 * When a field is given, the response also carries {@code errors: [{ field, message }]}
 * so clients can highlight the offending input, exactly like validation errors.
 */
public class BadRequestException extends RuntimeException {

	private final String field;

	public BadRequestException(String message) {
		this(null, message);
	}

	public BadRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return this.field;
	}

}
