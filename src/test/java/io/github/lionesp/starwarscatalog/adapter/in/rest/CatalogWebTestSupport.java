package io.github.lionesp.starwarscatalog.adapter.in.rest;

import io.github.lionesp.starwarscatalog.application.CacheAdminService;
import io.github.lionesp.starwarscatalog.application.CatalogService;
import io.github.lionesp.starwarscatalog.domain.model.Film;
import io.github.lionesp.starwarscatalog.domain.model.Person;
import io.github.lionesp.starwarscatalog.domain.model.Starship;
import io.github.lionesp.starwarscatalog.domain.model.Vehicle;
import io.github.lionesp.starwarscatalog.security.TokenService;
import io.github.lionesp.testsupport.TestCredentials;
import io.github.lionesp.testsupport.TestTokens;
import io.github.lionesp.testsupport.WebTestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WebTestApplication.class, properties = TestCredentials.JWT_SECRET_PROPERTY)
@AutoConfigureMockMvc
abstract class CatalogWebTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	private TokenService tokenService;

	@MockitoBean
	protected CatalogService<Person> peopleService;

	@MockitoBean
	protected CatalogService<Film> filmsService;

	@MockitoBean
	protected CatalogService<Starship> starshipsService;

	@MockitoBean
	protected CatalogService<Vehicle> vehiclesService;

	@MockitoBean
	protected CacheAdminService cacheAdminService;

	protected String userToken() {
		return TestTokens.userBearer(tokenService);
	}

	protected String adminToken() {
		return TestTokens.adminBearer(tokenService);
	}
}
