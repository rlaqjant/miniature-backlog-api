package com.rlaqjant.miniature_backlog_api.miniature.dto;

import com.rlaqjant.miniature_backlog_api.backlogitem.dto.BacklogItemResponse;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미니어처 상세 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiniatureDetailResponse {

    private Long id;
    private String title;
    private String description;
    private Boolean isPublic;
    private Integer progress;
    private List<BacklogItemResponse> backlogItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MiniatureDetailResponse of(
            Miniature miniature,
            int progress,
            List<BacklogItemResponse> backlogItems
    ) {
        return MiniatureDetailResponse.builder()
                .id(miniature.getId())
                .title(miniature.getTitle())
                .description(miniature.getDescription())
                .isPublic(miniature.getIsPublic())
                .progress(progress)
                .backlogItems(backlogItems)
                .createdAt(miniature.getCreatedAt())
                .updatedAt(miniature.getUpdatedAt())
                .build();
    }
}
