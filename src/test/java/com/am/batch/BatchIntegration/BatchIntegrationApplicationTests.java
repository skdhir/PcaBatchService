package com.am.batch.BatchIntegration;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.batch.app.BatchIntegrationApplication;

@SpringBootTest(classes = BatchIntegrationApplication.class)
class BatchIntegrationApplicationTests {

	@Test
	void contextLoads() {
	}
	
	@Before
	@After
	void cleanup() {
		System.out.println("Cleanup");
	}
	
	
	@Test
	void positiveIntegrationTest() {
		
	}

}
