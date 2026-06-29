package com.board.kimicetea.global.internal;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @InternalOnly 어노테이션이 붙은 메서드 진입 전 X-Internal-Key 헤더 검증
 * 주의: Spring MVC 요청 스레드에서만 동작 (RequestContextHolder 의존)
 */
@Aspect
@Component
public class InternalOnlyAspect {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Before("@annotation(InternalOnly)")
    public void validate() {
        final HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        final String key = request.getHeader(InternalApiConstants.INTERNAL_KEY_HEADER);
        if (key == null || !internalApiKey.equals(key)) {
            throw new CustomException(ErrorCode.INTERNAL_KEY_INVALID);
        }
    }
}
