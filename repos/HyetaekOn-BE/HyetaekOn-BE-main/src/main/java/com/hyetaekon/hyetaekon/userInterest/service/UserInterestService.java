package com.hyetaekon.hyetaekon.userInterest.service;

import com.hyetaekon.hyetaekon.userInterest.dto.CategorizedInterestsWithSelectionDto;
import com.hyetaekon.hyetaekon.userInterest.dto.InterestItemDto;
import com.hyetaekon.hyetaekon.userInterest.entity.UserInterest;
import com.hyetaekon.hyetaekon.userInterest.entity.UserInterestEnum;
import com.hyetaekon.hyetaekon.userInterest.repository.UserInterestRepository;
import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInterestService {
    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;

    // 모든 관심사 목록과 사용자 선택 여부 함께 조회
    @Transactional(readOnly = true)
    public CategorizedInterestsWithSelectionDto getUserInterestsWithSelection(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNullWithInterests(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 사용자가 선택한 관심사 목록
        List<String> selectedInterests = user.getInterests().stream()
            .map(UserInterest::getInterest)
            .toList();

        // 카테고리별로 모든 관심사를 포함하되, 선택 여부 표시
        Map<String, List<InterestItemDto>> result = new HashMap<>();

        Arrays.stream(UserInterestEnum.values())
            .forEach(interestEnum -> {
                String category = interestEnum.getCategory();
                String displayName = interestEnum.getDisplayName();
                boolean isSelected = selectedInterests.contains(displayName);

                if (!result.containsKey(category)) {
                    result.put(category, new ArrayList<>());
                }

                result.get(category).add(new InterestItemDto(displayName, isSelected));
            });

        log.debug("회원 관심사 조회 (선택 여부 포함) - 유저 ID: {}", userId);
        return new CategorizedInterestsWithSelectionDto(result);
    }

    // 선택한 관심사 저장
    @Transactional
    public void saveUserInterests(Long userId, List<String> selectedInterests) {
        if (selectedInterests == null) {
            selectedInterests = new ArrayList<>(); // 빈 리스트로 초기화
        }

        // 최대 선택 개수 검증
        if (selectedInterests.size() > 5) {
            throw new GlobalException(ErrorCode.INTEREST_LIMIT_EXCEEDED);
        }

        // 유효한 관심사인지 검증
        Set<String> validInterests = Arrays.stream(UserInterestEnum.values())
            .map(UserInterestEnum::getDisplayName)
            .collect(Collectors.toSet());

        for (String interest : selectedInterests) {
            if (!validInterests.contains(interest)) {
                throw new GlobalException(ErrorCode.INVALID_INTEREST);
            }
        }

        User user = userRepository.findByIdAndDeletedAtIsNullWithInterests(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 기존 관심사 제거
        user.getInterests().clear();

        // 새 관심사 추가
        for (String interest : selectedInterests) {
            UserInterest newInterest = UserInterest.builder()
                .user(user)
                .interest(interest)
                .build();
            userInterestRepository.save(newInterest);
        }

        log.debug("회원 관심사 갱신 - 유저 ID: {}, 선택 관심사: {}", userId, selectedInterests);
    }

}
