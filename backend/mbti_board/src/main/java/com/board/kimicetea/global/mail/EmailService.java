package com.board.kimicetea.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스
 * 주의: 실제 SMTP 연결은 send() 호출 시점에 발생 — 테스트에서는 @MockBean으로 대체
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /** 회원가입 이메일 인증 링크 발송 */
    public void sendEmailVerification(final String to, final String verificationLink) {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[MBTI Board] 이메일 인증");
        message.setText("""
                아래 링크를 클릭해 이메일 인증을 완료하세요. (10분 이내)

                %s

                요청하지 않은 경우 이 메일을 무시하세요.
                """.formatted(verificationLink));
        mailSender.send(message);
    }

    /** 회원 탈퇴 OTP 발송 */
    public void sendWithdrawalOtp(final String to, final String otp) {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[MBTI Board] 회원 탈퇴 인증 코드");
        message.setText("""
                회원 탈퇴 인증 코드: %s

                3분 이내에 입력해 주세요.
                """.formatted(otp));
        mailSender.send(message);
    }
}
