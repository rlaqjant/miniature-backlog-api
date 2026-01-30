package com.rlaqjant.miniature_backlog_api.progresslog.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 진행 로그 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressLogUpdateRequest {

    @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    private String content;

    private Boolean isPublic;
}
