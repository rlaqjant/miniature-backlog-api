package com.rlaqjant.miniature_backlog_api.progresslog.repository;

import com.rlaqjant.miniature_backlog_api.progresslog.domain.ProgressLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 진행 로그 Repository
 */
@Repository
public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {

    /**
     * 특정 미니어처의 진행 로그 목록 조회 (페이지네이션)
     */
    Page<ProgressLog> findByMiniatureIdOrderByCreatedAtDesc(Long miniatureId, Pageable pageable);

    /**
     * 공개 진행 로그 목록 조회 (페이지네이션)
     */
    Page<ProgressLog> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 미니어처의 공개 진행 로그 목록 조회 (페이지네이션)
     */
    Page<ProgressLog> findByMiniatureIdAndIsPublicTrueOrderByCreatedAtDesc(Long miniatureId, Pageable pageable);

    /**
     * 미니어처 ID로 진행 로그 목록 조회
     */
    List<ProgressLog> findByMiniatureId(Long miniatureId);

    /**
     * 미니어처 ID로 진행 로그 일괄 삭제
     */
    void deleteByMiniatureId(Long miniatureId);
}
