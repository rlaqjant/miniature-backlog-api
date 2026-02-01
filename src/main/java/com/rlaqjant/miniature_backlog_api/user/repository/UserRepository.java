package com.rlaqjant.miniature_backlog_api.user.repository;

import com.rlaqjant.miniature_backlog_api.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 이메일로 검색 (페이지네이션) - 관리자용
     */
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /**
     * 닉네임으로 검색 (페이지네이션) - 관리자용
     */
    Page<User> findByNicknameContainingIgnoreCase(String nickname, Pageable pageable);

    /**
     * 이메일 AND 닉네임으로 검색 (페이지네이션) - 관리자용
     */
    Page<User> findByEmailContainingIgnoreCaseAndNicknameContainingIgnoreCase(
            String email, String nickname, Pageable pageable);

    /**
     * 닉네임으로 사용자 목록 조회 (미니어처 검색 시 userId 추출용) - 관리자용
     */
    List<User> findByNicknameContainingIgnoreCase(String nickname);

    /**
     * OAuth 프로바이더 + 프로바이더 ID로 사용자 조회
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickname(String nickname);
}
