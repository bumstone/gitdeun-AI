package com.hyetaekon.hyetaekon.publicservice.controller;

import com.hyetaekon.hyetaekon.publicservice.dto.FilterOptionDto;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceDetailResponseDto;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.service.PublicServiceHandler;
import com.hyetaekon.hyetaekon.common.util.AuthenticateUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Validated
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class PublicServiceController {
    private final PublicServiceHandler publicServiceHandler;
    private final AuthenticateUser authenticateUser;

    // 전체 공공서비스 목록 조회(페이징, 정렬, 필터링)
    @GetMapping
    public ResponseEntity<Page<PublicServiceListResponseDto>> getAllServices(
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) List<String> specialGroups,
        @RequestParam(required = false) List<String> familyTypes,
        @RequestParam(required = false) List<String> categories,
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "9") @Positive @Max(50) int size) {

        Long userId = authenticateUser.authenticateUserId();

        return ResponseEntity.ok(publicServiceHandler.getAllServices(
            sort, specialGroups, familyTypes, categories, PageRequest.of(page, size), userId));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<FilterOptionDto>>> getFilterOptions() {
        return ResponseEntity.ok(publicServiceHandler.getFilterOptions());
    }

    // 서비스 분야별 공공서비스 목록 조회
    /*@GetMapping("/category/{category}")
    public ResponseEntity<Page<PublicServiceListResponseDto>> getServicesByCategory (
        @PathVariable("category") String categoryName,
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "9") @Positive @Max(30) int size) {
        Long userId = authenticateUser.authenticateUserId();
        ServiceCategory category = publicServiceHandler.getServiceCategory(categoryName);
        return ResponseEntity.ok(publicServiceHandler.getServicesByCategory(category, PageRequest.of(page, size), userId));

    }*/

    // 공공서비스 상세 조회
    @GetMapping("/detail/{serviceId}")
    public ResponseEntity<PublicServiceDetailResponseDto> getServiceDetail (@PathVariable("serviceId") String serviceId) {
        Long userId = authenticateUser.authenticateUserId();

        return ResponseEntity.ok(publicServiceHandler.getServiceDetail(serviceId, userId));
    }

    // 인기 서비스 목록 조회(북마크 수) -> 6개 고정
    @GetMapping("/popular")
    public ResponseEntity<List<PublicServiceListResponseDto>> getPopularServices() {
        Long userId = authenticateUser.authenticateUserId();

        return ResponseEntity.ok(publicServiceHandler.getPopularServices(userId)); // 최대 6개로 제한
    }

}
