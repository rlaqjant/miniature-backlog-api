package com.rlaqjant.miniature_backlog_api.miniature.dto;

import com.rlaqjant.miniature_backlog_api.backlogitem.dto.BacklogItemResponse;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공개 미니어처 상세 응답 DTO (작성자 정보 포함)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicMiniatureDetailResponse {

    private Long id;
    private String title;
    private String description;
    private Boolean isPublic;
    private Integer progress;
    private String userNickname;
    private List<BacklogItemResponse> backlogItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PublicMiniatureDetailResponse of(
            Miniature miniature,
            int progress,
            String userNickname,
            List<BacklogItemResponse> backlogItems
    ) {
        return PublicMiniatureDetailResponse.builder()
                .id(miniature.getId())
                .title(miniature.getTitle())
                .description(miniature.getDescription())
                .isPublic(miniature.getIsPublic())
                .progress(progress)
                .userNickname(userNickname)
                .backlogItems(backlogItems)
                .createdAt(miniature.getCreatedAt())
                .updatedAt(miniature.getUpdatedAt())
                .build();
    }
}
