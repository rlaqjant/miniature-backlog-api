package com.rlaqjant.miniature_backlog_api.miniature.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 미니어처 수정 요청 DTO
 * 모든 필드가 optional (PATCH 시맨틱)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiniatureUpdateRequest {

    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    private String description;

    private Boolean isPublic;
}
