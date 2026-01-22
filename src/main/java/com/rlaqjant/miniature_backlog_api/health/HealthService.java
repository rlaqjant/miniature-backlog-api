package com.rlaqjant.miniature_backlog_api.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 헬스 체크 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final Optional<BuildProperties> buildProperties;

    /**
     * 서버 상태 조회
     */
    public HealthResponse getHealthStatus() {
        String dbStatus = checkDatabaseConnection();
        String version = buildProperties
                .map(BuildProperties::getVersion)
                .orElse("development");

        return HealthResponse.builder()
                .status("ok")
                .database(dbStatus)
                .version(version)
                .build();
    }

    /**
     * 데이터베이스 연결 확인
     */
    private String checkDatabaseConnection() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "connected";
        } catch (Exception e) {
            log.warn("Database connection failed: {}", e.getMessage());
            return "disconnected";
        }
    }
}
