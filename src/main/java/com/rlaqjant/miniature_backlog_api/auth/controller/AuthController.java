package com.rlaqjant.miniature_backlog_api.auth.controller;

import com.rlaqjant.miniature_backlog_api.auth.dto.AuthResponse;
import com.rlaqjant.miniature_backlog_api.auth.dto.LoginRequest;
import com.rlaqjant.miniature_backlog_api.auth.dto.RegisterRequest;
import com.rlaqjant.miniature_backlog_api.auth.service.AuthService;
import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.security.jwt.JwtCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 컨트롤러
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtCookieUtil jwtCookieUtil;

    /**
     * 회원가입
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", null));
    }

    /**
     * 로그인
     * POST /auth/login
     * 성공 시 JWT 토큰을 HttpOnly 쿠키로 전달, body에 사용자 정보 반환
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);

        ResponseCookie cookie = jwtCookieUtil.createAccessTokenCookie(
                result.tokenResponse().getAccessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("로그인 성공", result.authResponse()));
    }

    /**
     * 토큰 갱신
     * POST /auth/refresh
     * 쿠키에서 토큰을 읽어 갱신 후 새 쿠키로 전달, body에 사용자 정보 반환
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request) {
        String token = jwtCookieUtil.getTokenFromCookies(request);

        if (token == null || token.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        AuthService.LoginResult result = authService.refresh(token);

        ResponseCookie cookie = jwtCookieUtil.createAccessTokenCookie(
                result.tokenResponse().getAccessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("토큰 갱신 성공", result.authResponse()));
    }

    /**
     * 로그아웃
     * POST /auth/logout
     * 쿠키 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie cookie = jwtCookieUtil.createDeleteCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("로그아웃 성공", null));
    }
}
