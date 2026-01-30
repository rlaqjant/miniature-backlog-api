package com.rlaqjant.miniature_backlog_api.miniature.dto;

import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 공개 미니어처 목록 응답 DTO (작성자 정보 포함)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicMiniatureResponse {

    private Long id;
    private String title;
    private Boolean isPublic;
    private Integer progress;
    private String userNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity + 진행률 + 작성자 닉네임으로 Response 생성
     */
    public static PublicMiniatureResponse of(Miniature miniature, int progress, String userNickname) {
        return PublicMiniatureResponse.builder()
                .id(miniature.getId())
                .title(miniature.getTitle())
                .isPublic(miniature.getIsPublic())
                .progress(progress)
                .userNickname(userNickname)
                .createdAt(miniature.getCreatedAt())
                .updatedAt(miniature.getUpdatedAt())
                .build();
    }
}
