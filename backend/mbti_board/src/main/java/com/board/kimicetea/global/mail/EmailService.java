package com.board.kimicetea.global.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
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

    /** 회원가입 이메일 인증 링크 발송 */
    @Async
    public void sendEmailVerification(@NonNull final String to, @NonNull final String verificationLink) {
        send(to, "[" + appName + "] 이메일 인증", buildLinkEmail(
                "이메일 인증",
                "아래 버튼을 클릭해 이메일 인증을 완료하세요.",
                "10분 이내에 완료해야 합니다.",
                verificationLink,
                "이메일 인증하기"
        ));
    }

    /** 비밀번호 재설정 링크 발송 */
    @Async
    public void sendPasswordResetEmail(@NonNull final String to, @NonNull final String resetLink) {
        send(to, "[" + appName + "] 비밀번호 재설정", buildLinkEmail(
                "비밀번호 재설정",
                "아래 버튼을 클릭해 비밀번호를 재설정하세요.",
                "30분 이내에 완료해야 합니다.",
                resetLink,
                "비밀번호 재설정하기"
        ));
    }

    /** 복구 이메일 인증 링크 발송 — 마이페이지에서 복구 이메일 등록/변경 시 소유권 확인용 */
    @Async
    public void sendRecoveryEmailVerification(@NonNull final String to, @NonNull final String verificationLink) {
        send(to, "[" + appName + "] 복구 이메일 인증", buildLinkEmail(
                "복구 이메일 인증",
                "아래 버튼을 클릭해 계정 복구용 이메일 인증을 완료하세요.",
                "10분 이내에 완료해야 합니다.",
                verificationLink,
                "이메일 인증하기"
        ));
    }

    /** 회원 탈퇴 OTP 발송 — 동기 처리: 실패 즉시 HTTP 500 반환, 레이스 컨디션 방지 */
    public void sendWithdrawalOtp(@NonNull final String to, @NonNull final String otp) {
        send(to, "[" + appName + "] 회원 탈퇴 인증 코드", buildOtpEmail(otp));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /** 링크 버튼이 포함된 공통 HTML 템플릿 */
    @NonNull
    @SuppressWarnings("null") // String.formatted()는 null을 반환하지 않으나 ECJ 타입 시스템이 @NonNull로 인식하지 못함
    private String buildLinkEmail(
            @NonNull final String title,
            @NonNull final String description,
            @NonNull final String caution,
            @NonNull final String link,
            @NonNull final String buttonLabel
    ) {
        return """
                <div style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f9f9f9;border-radius:8px;">
                  <h2 style="color:#333;margin-bottom:8px;">%s</h2>
                  <p style="color:#555;margin-bottom:4px;">%s</p>
                  <p style="color:#888;font-size:13px;margin-bottom:24px;">%s</p>
                  <a href="%s"
                     style="display:inline-block;padding:12px 28px;background:#2E5C40;color:#fff;
                            text-decoration:none;border-radius:6px;font-weight:bold;">
                    %s
                  </a>
                  <p style="color:#999;font-size:12px;margin-top:24px;">
                    버튼이 동작하지 않으면 아래 링크를 복사해 브라우저에 붙여넣으세요.<br>
                    <a href="%s" style="color:#2E5C40;word-break:break-all;">%s</a>
                  </p>
                  <p style="color:#999;font-size:12px;">보낸 사람이 <strong>%s</strong>가 맞는지 꼭 확인하세요.</p>
                  <p style="color:#999;font-size:12px;">요청하지 않은 경우 이 메일을 무시하세요.</p>
                </div>
                """.formatted(title, description, caution, link, buttonLabel, link, link, from);
    }

    /** OTP 인증 코드 HTML 템플릿 */
    @NonNull
    @SuppressWarnings("null") // String.formatted()는 null을 반환하지 않으나 ECJ 타입 시스템이 @NonNull로 인식하지 못함
    private String buildOtpEmail(@NonNull final String otp) {
        return """
                <div style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f9f9f9;border-radius:8px;">
                  <h2 style="color:#333;margin-bottom:8px;">회원 탈퇴 인증 코드</h2>
                  <p style="color:#555;margin-bottom:24px;">아래 코드를 입력해 탈퇴를 진행하세요. <strong>3분 이내</strong>에 입력해야 합니다.</p>
                  <div style="font-size:32px;font-weight:bold;letter-spacing:8px;text-align:center;
                              padding:20px;background:#fff;border:2px solid #e0e0e0;border-radius:6px;color:#333;">
                    %s
                  </div>
                  <p style="color:#999;font-size:12px;margin-top:24px;">보낸 사람이 <strong>%s</strong>가 맞는지 꼭 확인하세요.</p>
                  <p style="color:#999;font-size:12px;">요청하지 않은 경우 이 메일을 무시하세요.</p>
                </div>
                """.formatted(otp, from);
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
