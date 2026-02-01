package com.rlaqjant.miniature_backlog_api.auth.controller;

import com.rlaqjant.miniature_backlog_api.auth.dto.AuthResponse;
import com.rlaqjant.miniature_backlog_api.auth.dto.LoginRequest;
import com.rlaqjant.miniature_backlog_api.auth.dto.NicknameCheckResponse;
import com.rlaqjant.miniature_backlog_api.auth.dto.NicknameRequest;
import com.rlaqjant.miniature_backlog_api.auth.dto.RegisterRequest;
import com.rlaqjant.miniature_backlog_api.auth.service.AuthService;
import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.security.jwt.JwtCookieUtil;
import com.rlaqjant.miniature_backlog_api.security.oauth.GoogleOAuthService;
import com.rlaqjant.miniature_backlog_api.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 인증 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String OAUTH_STATE_COOKIE = "oauth_state";

    private final AuthService authService;
    private final JwtCookieUtil jwtCookieUtil;
    private final GoogleOAuthService googleOAuthService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:None}")
    private String cookieSameSite;

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
     * 닉네임 중복 확인
     * GET /auth/check-nickname?nickname=xxx
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(
            @RequestParam String nickname) {
        NicknameCheckResponse result = authService.checkNicknameAvailable(nickname);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
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
     * Google OAuth 로그인 시작
     * GET /auth/google/login
     * state 생성 후 Google OAuth 동의 화면으로 리다이렉트
     */
    @GetMapping("/google/login")
    public void googleLogin(HttpServletResponse response) throws Exception {
        String state = UUID.randomUUID().toString();

        // state를 httpOnly 쿠키에 저장 (CSRF 방지)
        Cookie stateCookie = new Cookie(OAUTH_STATE_COOKIE, state);
        stateCookie.setHttpOnly(true);
        stateCookie.setSecure(cookieSecure);
        stateCookie.setPath("/");
        stateCookie.setMaxAge(300); // 5분

        response.addCookie(stateCookie);

        String authUrl = googleOAuthService.generateAuthUrl(state);
        response.sendRedirect(authUrl);
    }

    /**
     * Google OAuth 콜백
     * GET /auth/oauth2/callback/google
     * authorization code로 토큰 교환 후 프론트엔드로 리다이렉트
     */
    @GetMapping("/oauth2/callback/google")
    public void googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        // state 검증 (CSRF 방지)
        String savedState = getOAuthStateCookie(request);
        if (savedState == null || !savedState.equals(state)) {
            log.warn("OAuth state 불일치: saved={}, received={}", savedState, state);
            response.sendRedirect(frontendUrl + "/login?error=" +
                    URLEncoder.encode("인증 상태가 유효하지 않습니다.", StandardCharsets.UTF_8));
            return;
        }

        // state 쿠키 삭제
        Cookie deleteStateCookie = new Cookie(OAUTH_STATE_COOKIE, "");
        deleteStateCookie.setHttpOnly(true);
        deleteStateCookie.setSecure(cookieSecure);
        deleteStateCookie.setPath("/");
        deleteStateCookie.setMaxAge(0);
        response.addCookie(deleteStateCookie);

        try {
            // authorization code → 사용자 정보 교환
            GoogleOAuthService.GoogleUserInfo userInfo = googleOAuthService.exchangeCodeForUserInfo(code);

            // 사용자 생성/조회 + JWT 발급
            AuthService.LoginResult result = authService.googleAuth(userInfo.email(), userInfo.googleId());

            // JWT 쿠키 설정
            ResponseCookie jwtCookie = jwtCookieUtil.createAccessTokenCookie(
                    result.tokenResponse().getAccessToken());
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

            // 프론트엔드 콜백 페이지로 리다이렉트
            response.sendRedirect(frontendUrl + "/auth/callback");

        } catch (BusinessException e) {
            log.error("Google OAuth 콜백 처리 실패: {}", e.getMessage());
            response.sendRedirect(frontendUrl + "/login?error=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * 요청에서 OAuth state 쿠키 추출
     */
    private String getOAuthStateCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (OAUTH_STATE_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 닉네임 설정
     * PATCH /auth/nickname
     * 인증 필요 - Google OAuth 신규 가입 후 닉네임 설정
     */
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<AuthResponse>> setNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NicknameRequest request) {
        AuthResponse response = authService.setNickname(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("닉네임 설정 완료", response));
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
