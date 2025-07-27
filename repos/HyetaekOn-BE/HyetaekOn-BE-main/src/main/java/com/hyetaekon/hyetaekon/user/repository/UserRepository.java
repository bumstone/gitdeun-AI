package com.hyetaekon.hyetaekon.user.repository;

import com.hyetaekon.hyetaekon.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository  extends JpaRepository<User, Long> {
  // user id로 검색
  Optional<User> findByIdAndDeletedAtIsNull(Long id);

  @EntityGraph(attributePaths = {"interests"})
  @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<User> findByIdAndDeletedAtIsNullWithInterests(@Param("id") Long id);

  // user realId로 검색
  Optional<User> findByRealIdAndDeletedAtIsNull(String realId);

  // 아이디 또는 닉네임 중복 여부 확인
  @Query("SELECT u FROM User u WHERE (u.realId = :realId OR u.nickname = :nickname) AND u.deletedAt IS NULL")
  Optional<User> findByRealIdOrNicknameAndDeletedAtIsNull(@Param("realId") String realId, @Param("nickname") String nickname);

  // 닉네임 중복 확인
  boolean existsByNickname(String nickname);

  // 이메일 중복 확인
  boolean existsByRealIdAndDeletedAtIsNull(String realId);

  // 정지된 회원 목록 조회
  @Query("SELECT u FROM User u WHERE u.suspendEndAt > :now AND u.deletedAt IS NULL")
  Page<User> findSuspendedUsers(Pageable pageable, @Param("now") LocalDateTime now);

  // 메서드 오버로딩: 현재 시간을 자동으로 설정
  default Page<User> findSuspendedUsers(Pageable pageable) {
    return findSuspendedUsers(pageable, LocalDateTime.now());
  }

  // 탈퇴한 회원 목록 조회
  @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL")
  Page<User> findWithdrawnUsers(Pageable pageable);
}
