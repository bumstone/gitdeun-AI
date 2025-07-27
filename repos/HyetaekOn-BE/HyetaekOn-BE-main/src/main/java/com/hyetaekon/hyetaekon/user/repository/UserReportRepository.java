package com.hyetaekon.hyetaekon.user.repository;


import com.hyetaekon.hyetaekon.user.entity.ReportStatus;
import com.hyetaekon.hyetaekon.user.entity.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    // 상태별 신고 목록 조회
    Page<UserReport> findByStatus(ReportStatus status, Pageable pageable);
}
