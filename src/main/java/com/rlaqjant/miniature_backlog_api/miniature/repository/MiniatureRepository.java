package com.rlaqjant.miniature_backlog_api.miniature.repository;

import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 미니어처 Repository
 */
@Repository
public interface MiniatureRepository extends JpaRepository<Miniature, Long> {

    /**
     * 사용자 ID로 미니어처 목록 조회 (생성일 내림차순)
     */
    List<Miniature> findByUserIdOrderByCreatedAtDesc(Long userId);
}
