package dev.woori.wooriLearn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WooriLearnApplicationTests {

	@Test
    @DisplayName("스프링 컨텍스트가 정상 기동된다")
	void contextLoads() {
	}

}
