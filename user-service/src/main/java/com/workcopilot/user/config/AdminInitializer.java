package com.workcopilot.user.config;

import com.workcopilot.user.entity.Role;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserSettings;
import com.workcopilot.user.entity.UserStatus;
import com.workcopilot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${app.admin-email:admin@workcopilot.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    if (user.getRole() != Role.ADMIN || user.getStatus() != UserStatus.ACTIVE) {
                        log.info("관리자 계정 권한/상태 업데이트: email={}", adminEmail);
                    }
                },
                () -> log.info("관리자 이메일 설정됨: {}. 해당 이메일로 OAuth 로그인 시 자동 ADMIN+ACTIVE 부여", adminEmail)
        );
    }
}
