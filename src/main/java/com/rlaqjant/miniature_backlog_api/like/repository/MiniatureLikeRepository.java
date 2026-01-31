package com.rlaqjant.miniature_backlog_api.like.repository;

import com.rlaqjant.miniature_backlog_api.like.domain.MiniatureLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 미니어처 좋아요 리포지토리
 */
public interface MiniatureLikeRepository extends JpaRepository<MiniatureLike, Long> {

    Optional<MiniatureLike> findByUserIdAndMiniatureId(Long userId, Long miniatureId);

    boolean existsByUserIdAndMiniatureId(Long userId, Long miniatureId);

    long countByMiniatureId(Long miniatureId);

    /**
     * 여러 미니어처의 좋아요 수를 일괄 조회 (N+1 방지)
     */
    @Query("SELECT ml.miniatureId, COUNT(ml) FROM MiniatureLike ml WHERE ml.miniatureId IN :miniatureIds GROUP BY ml.miniatureId")
    List<Object[]> countByMiniatureIdIn(@Param("miniatureIds") List<Long> miniatureIds);

    /**
     * 특정 사용자가 좋아요한 미니어처 ID 목록 조회 (N+1 방지)
     */
    @Query("SELECT ml.miniatureId FROM MiniatureLike ml WHERE ml.userId = :userId AND ml.miniatureId IN :miniatureIds")
    List<Long> findMiniatureIdsByUserIdAndMiniatureIdIn(@Param("userId") Long userId, @Param("miniatureIds") List<Long> miniatureIds);

    void deleteByUserIdAndMiniatureId(Long userId, Long miniatureId);

    void deleteByMiniatureId(Long miniatureId);
}
