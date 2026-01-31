package com.rlaqjant.miniature_backlog_api.like.dto;

import lombok.*;

/**
 * 좋아요 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeResponse {

    private boolean liked;
    private long likeCount;
}
