package com.rlaqjant.miniature_backlog_api.backlogitem.dto;

import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItem;
import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItemStatus;
import lombok.*;

/**
 * 백로그 항목 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacklogItemResponse {

    private Long id;
    private String stepName;
    private BacklogItemStatus status;
    private Integer orderIndex;
    private Integer progress;  // 미니어처 전체 진행률 (상태 변경 응답에서 사용)

    public static BacklogItemResponse from(BacklogItem backlogItem) {
        return BacklogItemResponse.builder()
                .id(backlogItem.getId())
                .stepName(backlogItem.getStepName())
                .status(backlogItem.getStatus())
                .orderIndex(backlogItem.getOrderIndex())
                .build();
    }
}
