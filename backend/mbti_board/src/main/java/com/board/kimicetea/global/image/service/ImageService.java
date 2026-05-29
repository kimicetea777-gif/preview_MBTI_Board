package com.board.kimicetea.global.image.service;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cloudflare R2 이미지 업로드 서비스 — Content-Type 헤더 + magic bytes 이중 검증 후 UUID 파일명으로 저장
 */
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    // 각 포맷의 파일 시그니처(magic bytes) — 클라이언트 Content-Type 스푸핑 방어
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/gif",  new byte[]{0x47, 0x49, 0x46, 0x38},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}    // "RIFF" + 4바이트 + "WEBP" 구조
    );

    private final S3Client r2Client;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-url}")
    private String publicUrl;

    /** MultipartFile을 R2에 업로드하고 공개 URL을 반환 */
    public String upload(final MultipartFile file) {
        final String contentType = validate(file);
        final String key = "images/" + UUID.randomUUID() + "." + extensionOf(contentType);
        try {
            r2Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return publicUrl + "/" + key;
    }

    /** 검증을 통과한 contentType을 반환 — null 체크 + MIME 검증 + 크기 + magic bytes */
    private String validate(final MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        final String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_TOO_LARGE);
        }
        if (!matchesMagicBytes(file, contentType)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        return contentType;
    }

    /** 파일 앞 바이트가 선언된 Content-Type의 시그니처와 일치하는지 확인 */
    private boolean matchesMagicBytes(final MultipartFile file, final String contentType) {
        final byte[] expected = MAGIC_BYTES.get(contentType);
        if (expected == null) return false;
        try (InputStream is = file.getInputStream()) {
            final byte[] header = new byte[12];
            final int read = is.read(header);
            if (read < expected.length) return false;
            for (int i = 0; i < expected.length; i++) {
                if (header[i] != expected[i]) return false;
            }
            // WebP: "RIFF" 뒤 4바이트(파일 크기)는 가변이므로, 8~11번째 바이트가 "WEBP"인지 확인
            if ("image/webp".equals(contentType)) {
                return read >= 12
                        && header[8] == 'W' && header[9] == 'E'
                        && header[10] == 'B' && header[11] == 'P';
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String extensionOf(final String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/gif"  -> "gif";
            case "image/webp" -> "webp";
            default -> throw new CustomException(ErrorCode.INVALID_IMAGE_TYPE);
        };
    }
}
