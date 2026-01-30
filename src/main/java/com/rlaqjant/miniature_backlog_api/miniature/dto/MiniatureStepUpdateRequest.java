package com.rlaqjant.miniature_backlog_api.miniature.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 미니어처 단계 일괄 변경 요청 DTO (칸반 드래그)
 */
@Getter
@NoArgsConstructor
public class MiniatureStepUpdateRequest {

    @NotBlank(message = "현재 단계는 필수입니다")
    private String currentStep; // "시작전", "언박싱", "조립", "프라이밍", "도색", "완료"
}
