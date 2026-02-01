package com.rlaqjant.miniature_backlog_api.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 닉네임 중복 확인 응답 DTO
 */
@Getter
@Builder
public class NicknameCheckResponse {

    /** 닉네임 사용 가능 여부 */
    private boolean available;

    /** 결과 메시지 */
    private String message;
}
