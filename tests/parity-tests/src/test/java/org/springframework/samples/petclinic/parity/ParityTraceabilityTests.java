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
package org.springframework.samples.petclinic.parity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Traceability guard: every business rule in {@code docs/agent/BUSINESS_RULES.md} must be
 * covered by at least one test whose name contains the rule id ({@code BR-OWN-05} →
 * {@code BR_OWN_05_...}).
 * <p>
 * Rules that intentionally have no automated test must be listed in
 * {@link #DOCUMENTED_EXCEPTIONS} with the reason, so the exception list stays alive
 * instead of silently growing.
 */
class ParityTraceabilityTests {

	/**
	 * Rules deliberately without a test, with the reason (kept in sync with
	 * MIGRATION.md).
	 */
	private static final Set<String> DOCUMENTED_EXCEPTIONS = Set.of("BR-VET-02b", // dead
																					// XML
																					// support
																					// removed,
																					// see
																					// MIGRATION
			"BR-PET-11", // in-memory de-duplication replaced by the unique constraint
			"BR-SYS-04", // database profiles: documented, not tested (H2 is the tested
							// one)
			"BR-SYS-05"); // actuator exposure is configuration, asserted in review

	private static final Pattern RULE_ROW = Pattern.compile("^\\|\\s*(BR-[A-Z]+-\\d+[a-z]?)\\s*\\|");

	private static final Path ROOT = Path.of("").toAbsolutePath().getParent().getParent();

	@Test
	void everyBusinessRuleHasATestWithItsId() throws IOException {
		Set<String> rules = rulesFromInventory();
		String testsSource = allTestSources();

		assertThat(rules).as("rules parsed from BUSINESS_RULES.md").hasSizeGreaterThan(30);

		List<String> uncovered = rules.stream()
			.filter(rule -> !DOCUMENTED_EXCEPTIONS.contains(rule))
			.filter(rule -> !testsSource.contains(rule.replace('-', '_')))
			.sorted()
			.toList();

		assertThat(uncovered).as("business rules without a BR_* test").isEmpty();
	}

	@Test
	void documentedExceptionsAreStillRealRules() throws IOException {
		Set<String> rules = rulesFromInventory();

		assertThat(DOCUMENTED_EXCEPTIONS).as("exceptions that no longer exist in the inventory")
			.allMatch(rules::contains);
	}

	private Set<String> rulesFromInventory() throws IOException {
		Set<String> rules = new LinkedHashSet<>();
		for (String line : Files.readAllLines(ROOT.resolve("docs/agent/BUSINESS_RULES.md"))) {
			Matcher matcher = RULE_ROW.matcher(line);
			if (matcher.find()) {
				rules.add(matcher.group(1));
			}
		}
		return rules;
	}

	private String allTestSources() throws IOException {
		StringBuilder sources = new StringBuilder();
		for (String module : List.of("services", "api-gateway", "libs")) {
			Path testsRoot = ROOT.resolve(module);
			if (!Files.isDirectory(testsRoot)) {
				continue;
			}
			try (Stream<Path> files = Files.walk(testsRoot)) {
				for (Path file : files.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> path.toString().contains("src" + java.io.File.separator + "test"))
					.toList()) {
					sources.append(Files.readString(file)).append('\n');
				}
			}
		}
		return sources.toString();
	}

}
