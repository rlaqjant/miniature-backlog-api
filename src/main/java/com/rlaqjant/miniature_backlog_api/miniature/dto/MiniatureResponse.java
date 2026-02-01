package com.rlaqjant.miniature_backlog_api.miniature.dto;

import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 미니어처 목록 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiniatureResponse {

    private Long id;
    private String title;
    private Boolean isPublic;
    private Integer progress;
    private String currentStep;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity + 진행률 + 현재 단계 + 썸네일 URL로 Response 생성
     */
    public static MiniatureResponse of(Miniature miniature, int progress, String currentStep, String thumbnailUrl) {
        return MiniatureResponse.builder()
                .id(miniature.getId())
                .title(miniature.getTitle())
                .isPublic(miniature.getIsPublic())
                .progress(progress)
                .currentStep(currentStep)
                .thumbnailUrl(thumbnailUrl)
                .createdAt(miniature.getCreatedAt())
                .updatedAt(miniature.getUpdatedAt())
                .build();
    }
}
