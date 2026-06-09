package one._026expo_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// 실제 env(${...}) 대신 테스트용 더미값(application-test.yml)으로 컨텍스트를 띄움
@ActiveProfiles("test")
@SpringBootTest
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
