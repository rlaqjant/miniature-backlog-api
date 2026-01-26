package com.rlaqjant.miniature_backlog_api.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 이미지 메타데이터 저장 요청 DTO
 * PRD 스펙: { progressLogId, objectKey }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageCreateRequest {

    @NotNull(message = "진행 로그 ID는 필수입니다.")
    private Long progressLogId;

    @NotBlank(message = "Object Key는 필수입니다.")
    @Size(max = 500, message = "Object Key는 500자 이하여야 합니다.")
    @Pattern(regexp = "^users/\\d+/[a-f0-9-]{36}\\.(png|jpg|jpeg|gif|webp)$",
            message = "유효하지 않은 Object Key 형식입니다.")
    private String objectKey;
}
