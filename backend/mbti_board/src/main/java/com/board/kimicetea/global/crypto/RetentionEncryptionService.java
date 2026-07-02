package com.board.kimicetea.global.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

/**
 * 법적 보존 로그용 가역 암호화 서비스 — AES-256/CBC + PKCS5
 * 주의: retention.encryption.password·salt는 반드시 환경변수로 주입 — 소스 하드코딩 금지
 *       salt는 16자 이상의 hex 문자열 (예: openssl rand -hex 16)
 */
@Service
public class RetentionEncryptionService {

    private final TextEncryptor encryptor;

    public RetentionEncryptionService(
            @Value("${retention.encryption.password}") final String password,
            @Value("${retention.encryption.salt}") final String salt) {
        this.encryptor = Encryptors.text(password, salt);
    }

    public String encrypt(final String plain) {
        return plain == null ? null : encryptor.encrypt(plain);
    }

    public String decrypt(final String cipher) {
        return cipher == null ? null : encryptor.decrypt(cipher);
    }
}
