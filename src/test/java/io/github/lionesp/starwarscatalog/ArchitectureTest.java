package io.github.lionesp.starwarscatalog;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.github.lionesp.starwarscatalog", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule E40_domainDoesNotDependOnFrameworksOrOuterLayers = noClasses()
		.that().resideInAPackage("..domain..")
		.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework..", "com.fasterxml.jackson..", "tools.jackson..", "..adapter..", "..application..");

	@ArchTest
	static final ArchRule E40_applicationDoesNotDependOnSpringOrAdapters = noClasses()
		.that().resideInAPackage("..application..")
		.should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..adapter..");
}
