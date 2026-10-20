package com.foodiefleet.app;

import com.foodiefleet.app.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// Also verifies that the Flyway migrations match the JPA entities (ddl-auto=validate)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AppApplicationTests {

	@MockBean
	private EmailService emailService;

	@Test
	void contextLoads() {
	}

}
