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
    private LocalDateTime createdAt;

    /**
     * Entity + 진행률로 Response 생성
     */
    public static MiniatureResponse of(Miniature miniature, int progress) {
        return MiniatureResponse.builder()
                .id(miniature.getId())
                .title(miniature.getTitle())
                .isPublic(miniature.getIsPublic())
                .progress(progress)
                .createdAt(miniature.getCreatedAt())
                .build();
    }
}
