package com.workcopilot.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@RequiredArgsConstructor
public class AuditLogAsyncWriter {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void write(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("감사 로그 저장: action={}, userId={}, result={}",
                    auditLog.getAction(), auditLog.getUserId(), auditLog.getResult());
        } catch (Exception e) {
            log.error("감사 로그 저장 실패: action={}, error={}",
                    auditLog.getAction(), e.getMessage());
        }
    }
}
