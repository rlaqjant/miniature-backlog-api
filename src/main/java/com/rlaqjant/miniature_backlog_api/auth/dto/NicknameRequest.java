package com.rlaqjant.miniature_backlog_api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 닉네임 설정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NicknameRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 50, message = "닉네임은 2~50자로 입력해주세요.")
    private String nickname;
}
