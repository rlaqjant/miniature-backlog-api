package com.rlaqjant.miniature_backlog_api.admin.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 관리자용 미니어처 페이지 응답 DTO
 */
@Getter
@Builder
public class AdminMiniaturePageResponse {

    private List<AdminMiniatureResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static AdminMiniaturePageResponse from(Page<AdminMiniatureResponse> page) {
        return AdminMiniaturePageResponse.builder()
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
