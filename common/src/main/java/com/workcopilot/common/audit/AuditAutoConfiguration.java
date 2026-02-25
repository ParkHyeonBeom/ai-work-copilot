package com.workcopilot.common.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@AutoConfiguration
@EnableAsync
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnBean(AuditLogRepository.class)
    public AuditLogAsyncWriter auditLogAsyncWriter(AuditLogRepository auditLogRepository) {
        return new AuditLogAsyncWriter(auditLogRepository);
    }

    @Bean
    @ConditionalOnBean(AuditLogAsyncWriter.class)
    public AuditAspect auditAspect(AuditLogAsyncWriter auditLogAsyncWriter) {
        return new AuditAspect(auditLogAsyncWriter);
    }
}
