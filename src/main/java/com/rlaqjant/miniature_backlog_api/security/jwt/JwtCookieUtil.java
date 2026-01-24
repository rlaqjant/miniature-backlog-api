package com.rlaqjant.miniature_backlog_api.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * JWT 쿠키 유틸리티
 * HttpOnly 쿠키로 JWT 토큰을 안전하게 전달
 */
@Component
public class JwtCookieUtil {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityMs;

    @Value("${app.cookie.secure:true}")
    private boolean secure;

    @Value("${app.cookie.same-site:Strict}")
    private String sameSite;

    @Value("${app.cookie.domain:}")
    private String domain;

    /**
     * 액세스 토큰 쿠키 생성
     */
    public ResponseCookie createAccessTokenCookie(String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(accessTokenValidityMs / 1000)
                .sameSite(sameSite);

        if (domain != null && !domain.isEmpty()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    /**
     * 액세스 토큰 쿠키 삭제 (로그아웃용)
     */
    public ResponseCookie createDeleteCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite);

        if (domain != null && !domain.isEmpty()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    /**
     * 요청에서 액세스 토큰 쿠키 추출
     */
    public String getTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 응답에 쿠키 설정
     */
    public void addCookieToResponse(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
