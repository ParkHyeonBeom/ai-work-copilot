package com.workcopilot.briefing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BriefingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BriefingServiceApplication.class, args);
    }
}
