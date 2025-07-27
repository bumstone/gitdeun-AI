package com.hyetaekon.hyetaekon.userInterest.repository;

import com.hyetaekon.hyetaekon.userInterest.entity.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
    /**
     * 사용자 ID에 해당하는 모든 관심사 항목을 조회
     */
    List<UserInterest> findByUserId(Long userId);


}
