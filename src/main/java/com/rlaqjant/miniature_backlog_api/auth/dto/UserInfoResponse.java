package com.rlaqjant.miniature_backlog_api.auth.dto;

import com.rlaqjant.miniature_backlog_api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 * 로그인/토큰 갱신 시 반환
 */
@Getter
@Builder
public class UserInfoResponse {

    private Long id;
    private String email;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;

    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
