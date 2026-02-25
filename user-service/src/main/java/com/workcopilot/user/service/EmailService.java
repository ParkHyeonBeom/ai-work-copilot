package com.workcopilot.user.service;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender.orElse(null);
        if (this.mailSender != null) {
            log.info("EmailService 초기화: JavaMailSender 활성");
        } else {
            log.info("EmailService 초기화: JavaMailSender 미설정 (로그 출력 모드)");
        }
    }

    public void sendVerificationEmail(String to, String code) {
        if (mailSender == null) {
            log.info("[로컬 모드] 인증 이메일 발송 대체 - to={}, code={}", to, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[AI Work Copilot] 이메일 인증코드");
            message.setText(String.format(
                    "안녕하세요.\n\n관리자가 귀하의 가입을 승인했습니다.\n" +
                    "아래 인증코드를 입력하여 계정을 활성화하세요.\n\n" +
                    "인증코드: %s\n\n" +
                    "이 코드는 10분간 유효합니다.", code));

            mailSender.send(message);
            log.info("인증 이메일 발송 완료: to={}", to);
        } catch (Exception e) {
            log.warn("이메일 발송 실패 (로그로 대체): to={}, code={}, error={}", to, code, e.getMessage());
        }
    }
}
