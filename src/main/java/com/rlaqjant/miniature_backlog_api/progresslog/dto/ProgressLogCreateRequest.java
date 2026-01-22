package com.rlaqjant.miniature_backlog_api.progresslog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 진행 로그 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressLogCreateRequest {

    @NotNull(message = "미니어처 ID는 필수입니다.")
    private Long miniatureId;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    private String content;

    private Boolean isPublic;
}
