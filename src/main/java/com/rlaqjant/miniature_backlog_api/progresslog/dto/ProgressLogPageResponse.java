package com.rlaqjant.miniature_backlog_api.progresslog.dto;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 진행 로그 페이지 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressLogPageResponse {

    private List<ProgressLogResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static ProgressLogPageResponse from(Page<ProgressLogResponse> page) {
        return ProgressLogPageResponse.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
