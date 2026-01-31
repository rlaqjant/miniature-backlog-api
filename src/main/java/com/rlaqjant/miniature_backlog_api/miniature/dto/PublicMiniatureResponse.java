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
    private Long likeCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity + 진행률 + 작성자 닉네임 + 좋아요 정보로 Response 생성
     */
    public static PublicMiniatureResponse of(Miniature miniature, int progress, String userNickname, long likeCount, boolean liked) {
        return PublicMiniatureResponse.builder()
                .id(miniature.getId())
                .title(miniature.getTitle())
                .isPublic(miniature.getIsPublic())
                .progress(progress)
                .userNickname(userNickname)
                .likeCount(likeCount)
                .liked(liked)
                .createdAt(miniature.getCreatedAt())
                .updatedAt(miniature.getUpdatedAt())
                .build();
    }
}
