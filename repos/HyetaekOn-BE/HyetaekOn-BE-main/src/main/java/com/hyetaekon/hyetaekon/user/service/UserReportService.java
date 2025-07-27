package com.hyetaekon.hyetaekon.user.service;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.user.dto.UserReportRequestDto;
import com.hyetaekon.hyetaekon.user.entity.ReportStatus;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.entity.UserReport;
import com.hyetaekon.hyetaekon.user.repository.UserReportRepository;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserReportService {
    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;

    // 사용자 신고
    @Transactional
    public void reportUser(Long reporterId, UserReportRequestDto reportRequestDto) {
        // 신고자 확인
        User reporter = userRepository.findByIdAndDeletedAtIsNull(reporterId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 신고 대상자 확인
        User reported = userRepository.findByIdAndDeletedAtIsNull(reportRequestDto.getReportedUserId())
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 자기 자신 신고 방지
        if (reporter.getId().equals(reported.getId())) {
            throw new GlobalException(ErrorCode.CANNOT_REPORT_SELF);
        }

        // 신고 내역 생성 및 저장
        UserReport userReport = UserReport.builder()
            .reporter(reporter)
            .reported(reported)
            .reason(reportRequestDto.getReason())
            .content(reportRequestDto.getContent())
            .status(ReportStatus.PENDING) // 대기 상태로 초기화
            .createdAt(LocalDateTime.now())
            .build();

        userReportRepository.save(userReport);
        log.info("사용자 신고 접수 완료 - 신고자: {}, 피신고자: {}", reporter.getId(), reported.getId());
    }
}