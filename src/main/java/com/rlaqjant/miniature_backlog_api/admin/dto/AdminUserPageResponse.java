package com.rlaqjant.miniature_backlog_api.admin.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 관리자용 사용자 페이지 응답 DTO
 */
@Getter
@Builder
public class AdminUserPageResponse {

    private List<AdminUserResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static AdminUserPageResponse from(Page<AdminUserResponse> page) {
        return AdminUserPageResponse.builder()
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
