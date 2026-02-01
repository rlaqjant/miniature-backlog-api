package com.rlaqjant.miniature_backlog_api.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증 응답 DTO
 * 로그인/토큰 갱신 시 사용자 정보를 래핑하여 반환
 */
@Getter
@Builder
public class AuthResponse {

    private UserInfoResponse user;

    /** Google OAuth 신규 가입 시 닉네임 설정 필요 여부 */
    @Builder.Default
    private boolean needsNickname = false;

    public static AuthResponse of(UserInfoResponse userInfo) {
        return AuthResponse.builder()
                .user(userInfo)
                .build();
    }

    public static AuthResponse ofNeedsNickname(UserInfoResponse userInfo) {
        return AuthResponse.builder()
                .user(userInfo)
                .needsNickname(true)
                .build();
    }
}
