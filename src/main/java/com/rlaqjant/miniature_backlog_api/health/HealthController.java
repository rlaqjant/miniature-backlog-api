package com.rlaqjant.miniature_backlog_api.health;

import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 헬스 체크 컨트롤러
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    /**
     * 헬스 체크 API
     * @return 서버 상태 정보
     */
    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(healthService.getHealthStatus());
    }
}
