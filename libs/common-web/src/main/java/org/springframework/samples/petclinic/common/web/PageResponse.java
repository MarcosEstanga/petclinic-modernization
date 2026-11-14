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

import java.util.List;

/**
 * Transport-neutral page envelope shared by every PetClinic service.
 * <p>
 * It deliberately does not depend on Spring Data: services map their own page objects
 * into this record, so the JSON contract stays identical across modules.
 *
 * @param content the elements of the requested page
 * @param page the 1-based page number this payload represents
 * @param size the requested page size
 * @param totalElements total number of elements across all pages
 * @param totalPages total number of pages
 * @param first whether this is the first page
 * @param last whether this is the last page
 * @param <T> element type
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean first,
		boolean last) {

	public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
		int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
		return new PageResponse<>(content, page, size, totalElements, totalPages, page <= 1, page >= totalPages);
	}

}
