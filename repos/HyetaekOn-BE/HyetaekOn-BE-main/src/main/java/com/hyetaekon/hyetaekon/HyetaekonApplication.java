package com.hyetaekon.hyetaekon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableJpaAuditing
@SpringBootApplication
@EnableScheduling
@EnableMongoRepositories
public class HyetaekonApplication {

	public static void main(String[] args) {
		SpringApplication.run(HyetaekonApplication.class, args);
	}

}
