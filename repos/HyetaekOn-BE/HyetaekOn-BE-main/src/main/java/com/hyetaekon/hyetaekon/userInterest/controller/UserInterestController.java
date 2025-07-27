package com.hyetaekon.hyetaekon.userInterest.controller;

import com.hyetaekon.hyetaekon.userInterest.dto.CategorizedInterestsResponseDto;
import com.hyetaekon.hyetaekon.userInterest.dto.CategorizedInterestsWithSelectionDto;
import com.hyetaekon.hyetaekon.userInterest.dto.InterestSelectionRequestDto;
import com.hyetaekon.hyetaekon.userInterest.entity.UserInterestEnum;
import com.hyetaekon.hyetaekon.userInterest.service.UserInterestService;
import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class UserInterestController {

    private final UserInterestService userInterestService;

    // 선택할 관심사 목록 조회
    @GetMapping
    public ResponseEntity<CategorizedInterestsResponseDto> getAvailableInterests() {
        // Enum에서 카테고리별 displayName 값 추출
        Map<String, List<String>> categorizedInterests = Arrays.stream(UserInterestEnum.values())
            .collect(Collectors.groupingBy(
                UserInterestEnum::getCategory,
                Collectors.mapping(UserInterestEnum::getDisplayName, Collectors.toList())
            ));
        return ResponseEntity.ok(new CategorizedInterestsResponseDto(categorizedInterests));
    }

    // 선택한 관심사 목록 조회
    @GetMapping("/me")
    public ResponseEntity<CategorizedInterestsWithSelectionDto> getMyInterestsWithSelection(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(userInterestService.getUserInterestsWithSelection(userId));
    }

    // 선택한 관심사 저장
    @PostMapping("/me")
    public ResponseEntity<Void> saveInterests(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody InterestSelectionRequestDto requestDto
    ) {
        Long userId = userDetails.getId();
        userInterestService.saveUserInterests(userId, requestDto.getAllInterests());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
