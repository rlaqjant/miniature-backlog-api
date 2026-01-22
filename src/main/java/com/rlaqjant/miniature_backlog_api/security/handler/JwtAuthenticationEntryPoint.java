package com.rlaqjant.miniature_backlog_api.security.handler;

import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 인증 실패 핸들러
 * 인증이 필요한 리소스에 인증 없이 접근할 때 호출
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.error("인증 실패: {}", authException.getMessage());

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String jsonResponse = String.format(
                "{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"},\"timestamp\":\"%s\"}",
                errorCode.getCode(),
                errorCode.getMessage(),
                timestamp
        );

        response.getWriter().write(jsonResponse);
    }
}
