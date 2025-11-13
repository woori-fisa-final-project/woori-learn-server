package dev.woori.wooriLearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication

public class WooriLearnApplication {
	public static void main(String[] args) {
        SpringApplication.run(WooriLearnApplication.class, args);
	}
}
