package com.workcopilot.chat.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"com.workcopilot.chat", "com.workcopilot.common"})
@EnableJpaRepositories(basePackages = {"com.workcopilot.chat"})
public class JpaConfig {
}
