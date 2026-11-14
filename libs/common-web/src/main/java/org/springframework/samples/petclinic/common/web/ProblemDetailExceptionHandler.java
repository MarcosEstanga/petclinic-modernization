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

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates application exceptions into RFC 9457 {@link ProblemDetail} responses, so
 * every service answers errors with the same shape:
 *
 * <pre>
 * {
 *   "type": "about:blank#not-found",
 *   "title": "Resource not found",
 *   "status": 404,
 *   "detail": "Owner with id 999 not found",
 *   "instance": "/owners/999",
 *   "errors": [ { "field": "telephone", "message": "must match \d{10}" } ]
 * }
 * </pre>
 */
@RestControllerAdvice
public class ProblemDetailExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", ex.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "conflict", "Conflict", ex.getMessage(), request);
		addFieldError(problem, ex.getField(), ex.getMessage());
		return problem;
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "bad-request", "Invalid request", ex.getMessage(),
				request);
		addFieldError(problem, ex.getField(), ex.getMessage());
		return problem;
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	ProblemDetail handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "service-unavailable", "Service unavailable", ex.getMessage(),
				request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed",
				"The request body contains invalid fields", request);
		problem.setProperty("errors", toErrors(ex.getBindingResult().getFieldErrors()));
		return problem;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed",
				"The request parameters are invalid", request);
		List<ApiValidationError> errors = ex.getConstraintViolations()
			.stream()
			.map(violation -> new ApiValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		String detail = "Parameter '%s' with value '%s' has an invalid format".formatted(ex.getName(), ex.getValue());
		return problem(HttpStatus.BAD_REQUEST, "type-mismatch", "Invalid parameter", detail, request);
	}

	private ProblemDetail problem(HttpStatus status, String type, String title, String detail,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("about:blank#" + type));
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}

	private List<ApiValidationError> toErrors(List<FieldError> fieldErrors) {
		return fieldErrors.stream()
			.map(error -> new ApiValidationError(error.getField(), error.getDefaultMessage()))
			.toList();
	}

	private void addFieldError(ProblemDetail problem, String field, String message) {
		if (field != null) {
			problem.setProperty("errors", List.of(new ApiValidationError(field, message)));
		}
	}

	/**
	 * One invalid field inside a {@code ProblemDetail#errors} array.
	 *
	 * @param field the offending field or parameter
	 * @param message human readable explanation
	 */
	public record ApiValidationError(String field, String message) {
	}

}
