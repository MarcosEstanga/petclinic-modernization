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
 * Signals a business conflict, such as a duplicated unique key. Mapped to HTTP 409 by
 * {@link ProblemDetailExceptionHandler}.
 * <p>
 * When a field is given, the response also carries {@code errors: [{ field, message }]}.
 */
public class ConflictException extends RuntimeException {

	private final String field;

	public ConflictException(String message) {
		this(null, message);
	}

	public ConflictException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return this.field;
	}

}
