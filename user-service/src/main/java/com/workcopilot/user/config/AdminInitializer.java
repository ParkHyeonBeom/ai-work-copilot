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
                        user.promoteToAdmin();
                        userRepository.save(user);
                        log.info("관리자 계정 권한/상태 업데이트 완료: email={}", adminEmail);
                    } else {
                        log.info("관리자 계정 확인됨: email={}", adminEmail);
                    }
                },
                () -> {
                    User admin = userRepository.save(User.builder()
                            .email(adminEmail)
                            .name("Admin")
                            .role(Role.ADMIN)
                            .status(UserStatus.ACTIVE)
                            .settings(UserSettings.defaults())
                            .build());
                    log.info("관리자 계정 자동 생성 완료: email={}, id={}", adminEmail, admin.getId());
                }
        );
    }
}
