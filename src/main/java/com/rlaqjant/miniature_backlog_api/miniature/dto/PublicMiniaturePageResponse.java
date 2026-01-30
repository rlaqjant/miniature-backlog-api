package com.rlaqjant.miniature_backlog_api.miniature.dto;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 공개 미니어처 페이지 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicMiniaturePageResponse {

    private List<PublicMiniatureResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static PublicMiniaturePageResponse from(Page<PublicMiniatureResponse> page) {
        return PublicMiniaturePageResponse.builder()
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
