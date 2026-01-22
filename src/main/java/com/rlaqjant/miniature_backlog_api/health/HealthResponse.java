package com.rlaqjant.miniature_backlog_api.health;

import lombok.Builder;
import lombok.Getter;

/**
 * 헬스 체크 응답
 */
@Getter
@Builder
public class HealthResponse {

    private final String status;
    private final String database;
    private final String version;
}
