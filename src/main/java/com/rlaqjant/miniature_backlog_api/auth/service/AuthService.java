package com.rlaqjant.miniature_backlog_api.auth.service;

import com.rlaqjant.miniature_backlog_api.auth.dto.*;
import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.security.jwt.JwtTokenProvider;
import com.rlaqjant.miniature_backlog_api.user.domain.User;
import com.rlaqjant.miniature_backlog_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 닉네임 중복 확인
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 비밀번호 암호화 후 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);
        log.info("회원가입 완료: {}", request.getEmail());
    }

    /**
     * 닉네임 사용 가능 여부 확인
     */
    public NicknameCheckResponse checkNicknameAvailable(String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);
        if (exists) {
            return NicknameCheckResponse.builder()
                    .available(false)
                    .message("이미 사용 중인 닉네임입니다.")
                    .build();
        }
        return NicknameCheckResponse.builder()
                .available(true)
                .message("사용 가능한 닉네임입니다.")
                .build();
    }

    /**
     * 로그인
     * @return 토큰 + 사용자 정보
     */
    public LoginResult login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Google 계정인 경우 비밀번호 로그인 불가
        if (!"LOCAL".equals(user.getProvider())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS_WITH_DIFFERENT_PROVIDER);
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());

        log.info("로그인 성공: {}", request.getEmail());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        AuthResponse authResponse = AuthResponse.of(UserInfoResponse.from(user));

        return new LoginResult(tokenResponse, authResponse);
    }

    /**
     * Google OAuth 인증 (서버 사이드 Authorization Code 방식)
     * @return 토큰 + 사용자 정보
     */
    @Transactional
    public LoginResult googleAuth(String email, String googleId) {
        // 기존 Google 사용자 조회
        Optional<User> existingGoogleUser = userRepository.findByProviderAndProviderId("GOOGLE", googleId);

        User user;
        boolean needsNickname = false;

        if (existingGoogleUser.isPresent()) {
            // 기존 Google 사용자 → 로그인
            user = existingGoogleUser.get();
            needsNickname = user.getNickname() == null;
        } else {
            // 동일 이메일의 LOCAL 계정 존재 여부 확인
            Optional<User> existingEmailUser = userRepository.findByEmail(email);
            if (existingEmailUser.isPresent()) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS_WITH_DIFFERENT_PROVIDER);
            }

            // 신규 Google 사용자 생성
            user = User.builder()
                    .email(email)
                    .provider("GOOGLE")
                    .providerId(googleId)
                    .build();

            userRepository.save(user);
            needsNickname = true;
            log.info("Google OAuth 회원가입 완료: {}", email);
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .build();

        AuthResponse authResponse;
        if (needsNickname) {
            authResponse = AuthResponse.ofNeedsNickname(UserInfoResponse.from(user));
        } else {
            authResponse = AuthResponse.of(UserInfoResponse.from(user));
        }

        return new LoginResult(tokenResponse, authResponse);
    }

    /**
     * 닉네임 설정 (Google OAuth 신규 가입 후 최초 1회)
     */
    @Transactional
    public AuthResponse setNickname(Long userId, NicknameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 닉네임이 설정된 경우
        if (user.getNickname() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "닉네임이 이미 설정되어 있습니다.");
        }

        // 닉네임 중복 확인
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(request.getNickname());
        log.info("닉네임 설정 완료: userId={}, nickname={}", userId, request.getNickname());

        return AuthResponse.of(UserInfoResponse.from(user));
    }

    /**
     * 토큰 갱신
     * @return 토큰 + 사용자 정보
     */
    public LoginResult refresh(String token) {
        // 1. 갱신 가능 여부 확인
        if (!jwtTokenProvider.canRefresh(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 토큰에서 이메일 추출
        String email = jwtTokenProvider.getEmailFromExpiredToken(token);

        // 3. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);

        log.info("토큰 갱신 완료: {}", email);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(newAccessToken)
                .build();

        // 닉네임 미설정 시 needsNickname 표시
        boolean needsNickname = user.getNickname() == null;
        AuthResponse authResponse;
        if (needsNickname) {
            authResponse = AuthResponse.ofNeedsNickname(UserInfoResponse.from(user));
        } else {
            authResponse = AuthResponse.of(UserInfoResponse.from(user));
        }

        return new LoginResult(tokenResponse, authResponse);
    }

    /**
     * 로그인/갱신 결과 (토큰 + 사용자 정보)
     */
    public record LoginResult(TokenResponse tokenResponse, AuthResponse authResponse) {
    }
}
