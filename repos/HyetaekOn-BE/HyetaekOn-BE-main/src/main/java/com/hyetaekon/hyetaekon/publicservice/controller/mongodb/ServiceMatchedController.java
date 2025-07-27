package com.hyetaekon.hyetaekon.publicservice.controller.mongodb;

import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.service.mongodb.ServiceMatchedHandler;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/mongo/services/matched")
@RequiredArgsConstructor
public class ServiceMatchedController {
    private final ServiceMatchedHandler serviceMatchedHandler;

    /**
     * 사용자 맞춤 공공서비스 추천 API
     * 사용자 프로필 및 검색 기록 기반으로 개인화된 서비스 목록 추천
     */
    @GetMapping
    public ResponseEntity<List<PublicServiceListResponseDto>> getMatchedServices(
        @RequestParam(name = "size", defaultValue = "10") @Positive @Max(20) int size,
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 사용자 맞춤 추천 서비스 조회
        List<PublicServiceListResponseDto> matchedServices =
            serviceMatchedHandler.getPersonalizedServices(userDetails.getId(), size);

        return ResponseEntity.ok(matchedServices);
    }

}
