package io.github.lionesp.starwarscatalog;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lionesp.testsupport.TestCredentials;
import io.github.lionesp.testsupport.TestProperties;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = { TestCredentials.JWT_SECRET_PROPERTY,
		TestProperties.UNREACHABLE_SWAPI_BASE_URL_PROPERTY })
class TestSupportIsolationTest {

	private static final String TEST_CLASSES_DIRECTORY = "test-classes";

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void fullContextHasNoBeanDefinedInTestClasses() {
		assertThat(beanNamesDefinedInTestClasses()).isEmpty();
	}

	private List<String> beanNamesDefinedInTestClasses() {
		return Arrays.stream(applicationContext.getBeanDefinitionNames())
			.filter(beanName -> isLoadedFromTestClasses(applicationContext.getType(beanName)))
			.toList();
	}

	private static boolean isLoadedFromTestClasses(Class<?> beanType) {
		if (beanType == null) {
			return false;
		}
		CodeSource codeSource = beanType.getProtectionDomain().getCodeSource();
		return codeSource != null && codeSource.getLocation().getPath().contains(TEST_CLASSES_DIRECTORY);
	}

}
