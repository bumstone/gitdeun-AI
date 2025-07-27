package com.hyetaekon.hyetaekon.user.service;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.common.jwt.BlacklistService;
import com.hyetaekon.hyetaekon.common.jwt.RefreshTokenService;
import com.hyetaekon.hyetaekon.user.dto.*;
import com.hyetaekon.hyetaekon.user.entity.Role;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.entity.UserLevel;
import com.hyetaekon.hyetaekon.user.mapper.UserMapper;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistService blacklistService;


    // 회원 가입
    // TODO: Occupation, BusinessType 직업 정보 재확인
    @Transactional
    public UserSignUpResponseDto registerUser(UserSignUpRequestDto userSignUpRequestDto) {
        // 이메일 또는 닉네임 중복 검사
        Optional<User> existingUser = userRepository.findByRealIdOrNicknameAndDeletedAtIsNull(
            userSignUpRequestDto.getRealId(),
            userSignUpRequestDto.getNickname()
        );

        // 비밀번호와 비밀번호 확인 일치 여부 검사
        if (!userSignUpRequestDto.getPassword().equals(userSignUpRequestDto.getConfirmPassword())) {
            throw new GlobalException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (existingUser.isPresent()) {
            User user = existingUser.get(); // NPE 방지
            if (user.getRealId().equals(userSignUpRequestDto.getRealId())) {
                throw new GlobalException(ErrorCode.DUPLICATED_REAL_ID);
            }
            if (user.getNickname().equals(userSignUpRequestDto.getNickname())) {
                throw new GlobalException(ErrorCode.DUPLICATED_NICKNAME);
            }
        }

        String encodedPassword = passwordEncoder.encode(userSignUpRequestDto.getPassword());

        // 추가 필드를 포함한 User 객체 생성
        User newUser = User.builder()
            .realId(userSignUpRequestDto.getRealId())
            .nickname(userSignUpRequestDto.getNickname())
            .password(encodedPassword)
            .name(userSignUpRequestDto.getName())
            .birthAt(userSignUpRequestDto.getBirthAt())
            .gender(userSignUpRequestDto.getGender())
            .city(userSignUpRequestDto.getCity())
            .state(userSignUpRequestDto.getState())
            .job(userSignUpRequestDto.getJob())
            .role(Role.ROLE_USER)
            .level(UserLevel.QUESTION_MARK)
            .point(0) // 초기 포인트 설정
            .createdAt(LocalDateTime.now()) // 생성 시간 설정
            .build();

        User savedUser = userRepository.save(newUser);
        log.debug("회원 가입 - 이메일: {}", savedUser.getRealId());

        return userMapper.toSignUpResponseDto(savedUser);
    }

    // 회원 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDto getMyInfo(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        return userMapper.toResponseDto(user);
    }

    // 이메일로 회원 검색
    @Transactional(readOnly = true)
    public User findUserByRealId(String realId) {
        return userRepository.findByRealIdAndDeletedAtIsNull(realId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_REAL_ID));

    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        return userMapper.toResponseDto(user);
    }


    // 회원 정보 수정(닉네임, 이름, 성별, 생년월일, 지역, 직업)
    @Transactional
    public UserResponseDto updateUserProfile(Long userId, UserProfileUpdateDto profileUpdateDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 닉네임 업데이트
        if (profileUpdateDto.getNickname() != null && !profileUpdateDto.getNickname().isBlank()) {
            // 현재 닉네임과 다를 경우에만 중복 체크
            if (!user.getNickname().equals(profileUpdateDto.getNickname()) &&
                userRepository.existsByNickname(profileUpdateDto.getNickname())) {
                throw new GlobalException(ErrorCode.DUPLICATED_NICKNAME);
            }
            user.updateNickname(profileUpdateDto.getNickname());
        }

        // 이름 업데이트
        if (profileUpdateDto.getName() != null && !profileUpdateDto.getName().isBlank()) {
            user.updateName(profileUpdateDto.getName());
        }

        // 생년월일 업데이트
        if (profileUpdateDto.getBirthAt() != null) {
            user.updateBirthAt(profileUpdateDto.getBirthAt());
        }

        // 성별 업데이트
        if (profileUpdateDto.getGender() != null && !profileUpdateDto.getGender().isBlank()) {
            user.updateGender(profileUpdateDto.getGender());
        }

        // 지역(시/도) 업데이트
        if (profileUpdateDto.getCity() != null && !profileUpdateDto.getCity().isBlank()) {
            user.updateCity(profileUpdateDto.getCity());
        }

        // 지역(시/군/구) 업데이트
        if (profileUpdateDto.getState() != null && !profileUpdateDto.getState().isBlank()) {
            user.updateState(profileUpdateDto.getState());
        }

        // 직업 업데이트 (필요시)
        if (profileUpdateDto.getJob() != null && !profileUpdateDto.getJob().isBlank()) {
            user.updateJob(profileUpdateDto.getJob());
        }

        User updatedUser = userRepository.save(user);
        log.debug("회원 프로필 정보 업데이트 - ID: {}", userId);
        return userMapper.toResponseDto(updatedUser);
    }

    // 비밀번호 변경
    @Transactional
    public void updateUserPassword(Long userId, UserPasswordUpdateDto passwordUpdateDto) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(passwordUpdateDto.getCurrentPassword(), user.getPassword())) {
            throw new GlobalException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 새 비밀번호와 확인 비밀번호 일치 여부 확인
        if (!passwordUpdateDto.getNewPassword().equals(passwordUpdateDto.getConfirmPassword())) {
            throw new GlobalException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        // 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (passwordEncoder.matches(passwordUpdateDto.getNewPassword(), user.getPassword())) {
            throw new GlobalException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        // 비밀번호 업데이트
        user.updatePassword(passwordEncoder.encode(passwordUpdateDto.getNewPassword()));
        userRepository.save(user);
        log.debug("회원 비밀번호 변경 완료 - ID: {}", userId);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser(Long userId, String deleteReason, String accessToken, String refreshToken) {
        // 사용자 정보 조회
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // redis에서 리프레시 토큰 삭제
        refreshTokenService.deleteRefreshToken(refreshToken);
        // access token 블랙리스트에 등록
        blacklistService.addToBlacklist(accessToken);

        // 사용자 탈퇴 처리 (소프트 삭제)
        user.deleteUser(deleteReason);
        log.debug("회원 탈퇴 - 이메일: {} , 탈퇴 사유: {}", userId, deleteReason);

    }

    // 중복 확인(회원 가입시 아이디, 닉네임 부분)
    public boolean checkDuplicate(String type, String value) {
        return switch (type.toLowerCase()) {
            case "realid" -> userRepository.existsByRealIdAndDeletedAtIsNull(value);
            case "nickname" -> userRepository.existsByNickname(value);
            default -> throw new IllegalArgumentException("잘못된 타입 입력값입니다.");
        };
    }

}
