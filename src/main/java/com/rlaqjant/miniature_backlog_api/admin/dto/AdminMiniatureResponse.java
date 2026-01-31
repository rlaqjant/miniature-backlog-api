package com.rlaqjant.miniature_backlog_api.admin.dto;

import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자용 미니어처 응답 DTO
 */
@Getter
@Builder
public class AdminMiniatureResponse {

    private Long id;
    private String title;
    private String userNickname;
    private Boolean isPublic;
    private int progress;
    private LocalDateTime createdAt;

    public static AdminMiniatureResponse of(Miniature miniature, String userNickname, int progress) {
        return AdminMiniatureResponse.builder()
                .id(miniature.getId())
                .title(miniature.getTitle())
                .userNickname(userNickname)
                .isPublic(miniature.getIsPublic())
                .progress(progress)
                .createdAt(miniature.getCreatedAt())
                .build();
    }
}
