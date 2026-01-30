package com.rlaqjant.miniature_backlog_api.miniature.repository;

import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 미니어처 Repository
 */
@Repository
public interface MiniatureRepository extends JpaRepository<Miniature, Long> {

    /**
     * 사용자 ID로 미니어처 목록 조회 (생성일 내림차순)
     */
    List<Miniature> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 공개 미니어처 목록 조회 (수정일 내림차순, 페이지네이션)
     */
    Page<Miniature> findByIsPublicTrueOrderByUpdatedAtDesc(Pageable pageable);

    /**
     * 공개 미니어처 단건 조회 (공개 상태 검증 포함)
     */
    Optional<Miniature> findByIdAndIsPublicTrue(Long id);
}
