package com.rlaqjant.miniature_backlog_api.backlogitem.dto;

import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 백로그 항목 상태 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacklogItemUpdateRequest {

    @NotNull(message = "상태는 필수입니다.")
    private BacklogItemStatus status;
}
