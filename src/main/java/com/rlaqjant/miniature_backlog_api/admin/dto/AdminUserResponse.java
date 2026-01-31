package com.rlaqjant.miniature_backlog_api.admin.dto;

import com.rlaqjant.miniature_backlog_api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자용 사용자 응답 DTO
 */
@Getter
@Builder
public class AdminUserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;
    private long miniatureCount;

    public static AdminUserResponse of(User user, long miniatureCount) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .miniatureCount(miniatureCount)
                .build();
    }
}
