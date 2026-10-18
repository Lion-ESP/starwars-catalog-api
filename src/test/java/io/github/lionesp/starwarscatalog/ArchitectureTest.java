package io.github.lionesp.starwarscatalog;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.github.lionesp.starwarscatalog", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String ADR = "ADR 0002: ";

	private static final String SECURITY_PACKAGE = "io.github.lionesp.starwarscatalog.security..";

	private static final String CONFIG_PACKAGE = "io.github.lionesp.starwarscatalog.config..";

	@ArchTest
	static final ArchRule domainDoesNotDependOnFrameworksOrOuterLayers = noClasses()
		.that().resideInAPackage("..domain..")
		.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework..", "com.fasterxml.jackson..", "tools.jackson..", "..adapter..", "..application..")
		.because(ADR + "domain is plain Java and knows nothing about frameworks or outer layers");

	@ArchTest
	static final ArchRule applicationDoesNotDependOnSpringJacksonOrAdapters = noClasses()
		.that().resideInAPackage("..application..")
		.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework..", "com.fasterxml.jackson..", "tools.jackson..", "..adapter..")
		.because(ADR + "application only knows the domain and its own ports, with no Spring or Jackson");

	@ArchTest
	static final ArchRule domainAndApplicationDoNotDependOnInfrastructureLibraries = noClasses()
		.that().resideInAnyPackage("..domain..", "..application..")
		.should().dependOnClassesThat().resideInAnyPackage(
				"jakarta..", "io.github.resilience4j..", "com.github.benmanes..", SECURITY_PACKAGE,
				CONFIG_PACKAGE)
		.because(ADR + "beans are wired in CatalogConfig and infrastructure stays in the adapters");

	@ArchTest
	static final ArchRule inboundAdaptersDoNotDependOnOutboundAdapters = noClasses()
		.that().resideInAPackage("..adapter.in..")
		.should().dependOnClassesThat().resideInAPackage("..adapter.out..")
		.because(ADR + "adapters talk to each other only through the application ports");

	@ArchTest
	static final ArchRule outboundAdaptersDoNotDependOnInboundAdaptersOrSecurity = noClasses()
		.that().resideInAPackage("..adapter.out..")
		.should().dependOnClassesThat().resideInAnyPackage("..adapter.in..", SECURITY_PACKAGE)
		.because(ADR + "adapters talk to each other only through the application ports");

}
