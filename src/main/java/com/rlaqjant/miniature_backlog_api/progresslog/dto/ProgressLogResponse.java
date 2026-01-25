package com.rlaqjant.miniature_backlog_api.progresslog.dto;

import com.rlaqjant.miniature_backlog_api.image.dto.ImageResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.domain.ProgressLog;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 진행 로그 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressLogResponse {

    private Long id;
    private Long miniatureId;
    private String miniatureTitle;
    private Long userId;
    private String userNickname;
    private String content;
    private Boolean isPublic;
    private List<ImageResponse> images;
    private LocalDateTime createdAt;

    public static ProgressLogResponse from(ProgressLog progressLog) {
        return ProgressLogResponse.builder()
                .id(progressLog.getId())
                .miniatureId(progressLog.getMiniatureId())
                .userId(progressLog.getUserId())
                .content(progressLog.getContent())
                .isPublic(progressLog.getIsPublic())
                .createdAt(progressLog.getCreatedAt())
                .build();
    }

    public static ProgressLogResponse of(ProgressLog progressLog, String miniatureTitle, String userNickname) {
        return ProgressLogResponse.builder()
                .id(progressLog.getId())
                .miniatureId(progressLog.getMiniatureId())
                .miniatureTitle(miniatureTitle)
                .userId(progressLog.getUserId())
                .userNickname(userNickname)
                .content(progressLog.getContent())
                .isPublic(progressLog.getIsPublic())
                .createdAt(progressLog.getCreatedAt())
                .build();
    }

    public static ProgressLogResponse of(ProgressLog progressLog, String miniatureTitle, String userNickname, List<ImageResponse> images) {
        return ProgressLogResponse.builder()
                .id(progressLog.getId())
                .miniatureId(progressLog.getMiniatureId())
                .miniatureTitle(miniatureTitle)
                .userId(progressLog.getUserId())
                .userNickname(userNickname)
                .content(progressLog.getContent())
                .isPublic(progressLog.getIsPublic())
                .images(images)
                .createdAt(progressLog.getCreatedAt())
                .build();
    }
}
