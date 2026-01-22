package com.rlaqjant.miniature_backlog_api.image.dto;

import com.rlaqjant.miniature_backlog_api.image.validator.AllowedContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Presigned URL 발급 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignRequest {

    @NotBlank(message = "파일명은 필수입니다.")
    @Size(max = 255, message = "파일명은 255자 이하여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣._-]+$", message = "파일명에 허용되지 않은 문자가 포함되어 있습니다.")
    private String fileName;

    @NotBlank(message = "Content-Type은 필수입니다.")
    @AllowedContentType
    private String contentType;
}
