package com.workcopilot.ai.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"com.workcopilot.ai", "com.workcopilot.common.audit"})
@EnableJpaRepositories(basePackages = {"com.workcopilot.ai", "com.workcopilot.common.audit"})
public class AuditJpaConfig {
}
