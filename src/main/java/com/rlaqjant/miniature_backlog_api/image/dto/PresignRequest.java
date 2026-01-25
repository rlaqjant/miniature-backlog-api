package com.rlaqjant.miniature_backlog_api.image.dto;

import com.rlaqjant.miniature_backlog_api.image.validator.AllowedContentType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Presigned URL 발급 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignRequest {

    @NotBlank(message = "Content-Type은 필수입니다.")
    @AllowedContentType
    private String contentType;
}
