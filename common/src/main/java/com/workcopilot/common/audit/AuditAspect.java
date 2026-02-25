package com.workcopilot.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogAsyncWriter auditLogAsyncWriter;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Long userId = extractUserId();
        String ipAddress = extractIpAddress();

        try {
            Object result = joinPoint.proceed();
            auditLogAsyncWriter.write(AuditLog.builder()
                    .userId(userId)
                    .action(audited.action())
                    .result(AuditResult.SUCCESS)
                    .detail(joinPoint.getSignature().toShortString())
                    .ipAddress(ipAddress)
                    .build());
            return result;
        } catch (Exception e) {
            auditLogAsyncWriter.write(AuditLog.builder()
                    .userId(userId)
                    .action(audited.action())
                    .result(AuditResult.FAILURE)
                    .detail(e.getMessage())
                    .ipAddress(ipAddress)
                    .build());
            throw e;
        }
    }

    private Long extractUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long) {
                return (Long) auth.getPrincipal();
            }
        } catch (Exception e) {
            log.debug("감사 로그 사용자 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("감사 로그 IP 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}
