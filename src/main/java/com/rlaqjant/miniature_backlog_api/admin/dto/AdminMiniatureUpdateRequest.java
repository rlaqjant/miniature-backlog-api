package com.rlaqjant.miniature_backlog_api.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자용 미니어처 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class AdminMiniatureUpdateRequest {

    private Boolean isPublic;
}
