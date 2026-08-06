package com.board.kimicetea.global.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스
 * 주의: 실제 SMTP 연결은 send() 호출 시점에 발생 — 테스트에서는 @MockBean으로 대체
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.name}")
    private String appName;

    /** 회원 탈퇴 OTP 발송 — 동기 처리: 실패 즉시 HTTP 500 반환, 레이스 컨디션 방지 */
    public void sendWithdrawalOtp(@NonNull final String to, @NonNull final String otp) {
        send(to, "[" + appName + "] 회원 탈퇴 인증 코드", buildOtpEmail(
                "회원 탈퇴 인증 코드",
                "아래 코드를 입력해 탈퇴를 진행하세요.",
                "3분 이내",
                otp
        ));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /** OTP 인증 코드 HTML 템플릿 */
    @NonNull
    @SuppressWarnings("null") // String.formatted()는 null을 반환하지 않으나 ECJ 타입 시스템이 @NonNull로 인식하지 못함
    private String buildOtpEmail(
            @NonNull final String title,
            @NonNull final String description,
            @NonNull final String validity,
            @NonNull final String otp
    ) {
        return """
                <div style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f9f9f9;border-radius:8px;">
                  <h2 style="color:#333;margin-bottom:8px;">%s</h2>
                  <p style="color:#555;margin-bottom:24px;">%s <strong>%s</strong>에 입력해야 합니다.</p>
                  <div style="font-size:32px;font-weight:bold;letter-spacing:8px;text-align:center;
                              padding:20px;background:#fff;border:2px solid #e0e0e0;border-radius:6px;color:#333;">
                    %s
                  </div>
                  <p style="color:#999;font-size:12px;margin-top:24px;">보낸 사람이 <strong>%s</strong>가 맞는지 꼭 확인하세요.</p>
                  <p style="color:#999;font-size:12px;">요청하지 않은 경우 이 메일을 무시하세요.</p>
                </div>
                """.formatted(title, description, validity, otp, from);
    }

    /** HTML 메일 발송 공통 처리 */
    private void send(@NonNull final String to, @NonNull final String subject, @NonNull final String html) {
        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage(), e);
        }
    }
}
