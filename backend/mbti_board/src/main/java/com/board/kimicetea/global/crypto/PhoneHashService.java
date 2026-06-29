package com.board.kimicetea.global.crypto;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 전화번호 HMAC-SHA256 해싱 서비스 — DB 조회 키로 사용 가능한 결정적 해시 생성
 * 주의: 시크릿 키 없이는 역추적 불가. 키 노출 시 phone_hash 전체 재해시 필요
 */
@Slf4j
@Service
public class PhoneHashService {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secretKeyBytes;

    public PhoneHashService(@Value("${phone.hmac-secret}") final String secret) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** 전화번호 → HMAC-SHA256 hex 문자열 (64자) */
    public String hash(final String phone) {
        try {
            final Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, ALGORITHM));
            final byte[] digest = mac.doFinal(normalize(phone).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            log.error("핸드폰 번호: HMAC-SHA256 해싱 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /** 하이픈 제거 정규화 (010-1234-5678 → 01012345678) */
    private String normalize(final String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}
