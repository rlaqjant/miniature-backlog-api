package com.rlaqjant.miniature_backlog_api.image.dto;

import lombok.*;

/**
 * Presigned URL 발급 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignResponse {

    private String uploadUrl;
    private String objectKey;
}
