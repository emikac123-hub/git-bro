package com.erik.git_bro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class GitBroApplicationTests {

	@Test
	void contextLoads() {
	}

}
