package com.rlaqjant.miniature_backlog_api.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자용 사용자 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class AdminUserUpdateRequest {

    private String role;
}
