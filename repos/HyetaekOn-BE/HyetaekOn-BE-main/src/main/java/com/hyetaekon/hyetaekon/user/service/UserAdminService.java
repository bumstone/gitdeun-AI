package com.hyetaekon.hyetaekon.user.service;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.user.dto.admin.UserAdminResponseDto;
import com.hyetaekon.hyetaekon.user.dto.admin.UserReportProcessDto;
import com.hyetaekon.hyetaekon.user.dto.admin.UserReportResponseDto;
import com.hyetaekon.hyetaekon.user.dto.admin.UserSuspendRequestDto;
import com.hyetaekon.hyetaekon.user.entity.ReportStatus;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.entity.UserReport;
import com.hyetaekon.hyetaekon.user.mapper.UserAdminMapper;
import com.hyetaekon.hyetaekon.user.repository.UserReportRepository;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {
    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;
    private final UserAdminMapper userAdminMapper;

    /**
     * 모든 회원 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponseDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(userAdminMapper::toAdminResponseDto);
    }

    /**
     * 회원 정지
     */
    @Transactional
    public void suspendUser(Long userId, UserSuspendRequestDto requestDto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 현재 시간보다 종료 시간이 빠른지 검사
        if (requestDto.getSuspendEndAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.INVALID_SUSPEND_TIME);
        }

        // 시작 시간이 종료 시간보다 늦은지 검사
        if (requestDto.getSuspendStartAt().isAfter(requestDto.getSuspendEndAt())) {
            throw new GlobalException(ErrorCode.INVALID_SUSPEND_TIME);
        }

        user.setSuspendStartAt(requestDto.getSuspendStartAt());
        user.setSuspendEndAt(requestDto.getSuspendEndAt());
        user.setSuspendReason(requestDto.getSuspendReason());

        userRepository.save(user);
        log.info("회원 {} 정지 처리 완료", userId);
    }

    /**
     * 정지 해제
     */
    @Transactional
    public void unsuspendUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 정지 상태가 아닌 경우
        if (user.getSuspendEndAt() == null || user.getSuspendEndAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.NOT_SUSPENDED_USER);
        }

        user.setSuspendStartAt(null);
        user.setSuspendEndAt(null);
        user.setSuspendReason(null);

        userRepository.save(user);
        log.info("회원 {} 정지 해제 완료", userId);
    }

    /**
     * 정지 회원 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponseDto> getSuspendedUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("suspendEndAt").descending());
        Page<User> userPage = userRepository.findSuspendedUsers(pageable);
        return userPage.map(userAdminMapper::toAdminResponseDto);
    }

    /**
     * 탈퇴 회원 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponseDto> getWithdrawnUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        Page<User> userPage = userRepository.findWithdrawnUsers(pageable);
        return userPage.map(userAdminMapper::toAdminResponseDto);
    }

    /**
     * 신고 내역 조회 (전체)
     */
    @Transactional(readOnly = true)
    public Page<UserReportResponseDto> getUserReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserReport> reportPage = userReportRepository.findAll(pageable);
        return reportPage.map(userAdminMapper::toReportResponseDto);
    }

    /**
     * 상태별 신고 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<UserReportResponseDto> getReportsByStatus(ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserReport> reportPage = userReportRepository.findByStatus(status, pageable);
        return reportPage.map(userAdminMapper::toReportResponseDto);
    }

    /**
     * 신고 승인 처리
     */
    @Transactional
    public void resolveReport(Long reportId, boolean suspendUser, UserSuspendRequestDto suspendRequestDto) {
        UserReport report = userReportRepository.findById(reportId)
            .orElseThrow(() -> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

        // 이미 처리된 신고인지 확인
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new GlobalException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        // 신고 승인 처리
        report.resolve();

        // 신고당한 사용자 정지 처리 여부 확인
        if (suspendUser && suspendRequestDto != null) {
            User reportedUser = report.getReported();
            suspendUser(reportedUser.getId(), suspendRequestDto);
            log.info("신고에 따른 사용자 {} 정지 처리 완료", reportedUser.getId());
        }

        userReportRepository.save(report);
        log.info("신고 {} 승인 처리 완료", reportId);
    }

    /**
     * 신고 거부 처리
     */
    @Transactional
    public void rejectReport(Long reportId) {
        UserReport report = userReportRepository.findById(reportId)
            .orElseThrow(() -> new GlobalException(ErrorCode.REPORT_NOT_FOUND));

        // 이미 처리된 신고인지 확인
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new GlobalException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        report.reject();
        userReportRepository.save(report);
        log.info("신고 {} 거부 처리 완료", reportId);
    }


}
