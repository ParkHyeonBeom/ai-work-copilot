package com.workcopilot.briefing.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"com.workcopilot.briefing", "com.workcopilot.common.audit"})
@EnableJpaRepositories(basePackages = {"com.workcopilot.briefing", "com.workcopilot.common.audit"})
public class AuditJpaConfig {
}
