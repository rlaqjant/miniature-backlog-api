package com.rlaqjant.miniature_backlog_api.miniature.repository;

import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 전체 미니어처 조회 (생성일 내림차순, 페이지네이션) - 관리자용
     */
    Page<Miniature> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 사용자별 미니어처 수 조회 - 관리자용
     */
    long countByUserId(Long userId);

    /**
     * 제목으로 검색 (생성일 내림차순, 페이지네이션) - 관리자용
     */
    Page<Miniature> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title, Pageable pageable);

    /**
     * userId 목록으로 검색 (생성일 내림차순, 페이지네이션) - 관리자용
     */
    Page<Miniature> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);

    /**
     * 제목 AND userId 목록으로 검색 (생성일 내림차순, 페이지네이션) - 관리자용
     */
    @Query("SELECT m FROM Miniature m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')) AND m.userId IN :userIds ORDER BY m.createdAt DESC")
    Page<Miniature> searchByTitleAndUserIds(@Param("title") String title, @Param("userIds") List<Long> userIds, Pageable pageable);
}
