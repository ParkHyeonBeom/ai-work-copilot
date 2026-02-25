package com.workcopilot.user.service;

import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final String KEY_PREFIX = "verify:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public String generateCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = KEY_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, TTL);
        log.info("인증코드 생성: email={}, TTL=10분", email);
        return code;
    }

    public void verifyCode(String email, String code) {
        String key = KEY_PREFIX + email;
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!stored.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        redisTemplate.delete(key);
        log.info("인증코드 검증 성공: email={}", email);
    }
}
