package com.rlaqjant.miniature_backlog_api.image.dto;

import com.rlaqjant.miniature_backlog_api.image.domain.Image;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이미지 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResponse {

    private Long id;
    private Long progressLogId;
    private String objectKey;
    private String fileName;
    private String contentType;
    private String imageUrl;
    private LocalDateTime createdAt;

    /**
     * 엔티티를 응답 DTO로 변환
     */
    public static ImageResponse from(Image image) {
        return ImageResponse.builder()
                .id(image.getId())
                .progressLogId(image.getProgressLogId())
                .objectKey(image.getObjectKey())
                .fileName(image.getFileName())
                .contentType(image.getContentType())
                .createdAt(image.getCreatedAt())
                .build();
    }

    /**
     * 엔티티를 응답 DTO로 변환 (이미지 URL 포함)
     */
    public static ImageResponse from(Image image, String imageUrl) {
        return ImageResponse.builder()
                .id(image.getId())
                .progressLogId(image.getProgressLogId())
                .objectKey(image.getObjectKey())
                .fileName(image.getFileName())
                .contentType(image.getContentType())
                .imageUrl(imageUrl)
                .createdAt(image.getCreatedAt())
                .build();
    }
}
