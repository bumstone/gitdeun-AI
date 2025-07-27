package com.hyetaekon.hyetaekon.publicservice.service;

import com.hyetaekon.hyetaekon.bookmark.repository.BookmarkRepository;
import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.publicservice.dto.FilterOptionDto;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceDetailResponseDto;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.entity.FamilyTypeEnum;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import com.hyetaekon.hyetaekon.publicservice.entity.ServiceCategory;
import com.hyetaekon.hyetaekon.publicservice.entity.SpecialGroupEnum;
import com.hyetaekon.hyetaekon.publicservice.mapper.PublicServiceMapper;
import com.hyetaekon.hyetaekon.publicservice.repository.PublicServiceRepository;
import com.hyetaekon.hyetaekon.publicservice.util.PublicServiceValidate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicServiceHandler {
    private final PublicServiceRepository publicServiceRepository;
    private final PublicServiceMapper publicServiceMapper;
    private final PublicServiceValidate publicServiceValidate;
    private final BookmarkRepository bookmarkRepository;
    private final RecentVisitService recentVisitService;

    // 서비스분야별 서비스목록 조회(페이지)
    /*public Page<PublicServiceListResponseDto> getServicesByCategory(ServiceCategory category, Pageable pageable, Long userId) {
        Page<PublicService> services = publicServiceRepository.findByServiceCategory(category, pageable);

        return services.map(service -> {
            PublicServiceListResponseDto dto = publicServiceMapper.toListDto(service);
            if (userId != 0L) {
                // 로그인한 사용자면 북마크 여부 확인
                dto.setBookmarked(bookmarkRepository.existsByUserIdAndPublicServiceId(userId, service.getId()));
            }
            return dto;
        });
    }*/

    /*    public ServiceCategory getServiceCategory(String categoryName) {
        return publicServiceValidate.validateServiceCategory(categoryName);
    }*/

    // 서비스 상세 조회
    @Transactional
    public PublicServiceDetailResponseDto getServiceDetail(String serviceId, Long userId) {
        PublicService service = publicServiceValidate.validateServiceById(serviceId);

        // 필수 필드가 null인지 검증하는 로직 추가
        if (publicServiceValidate.isDetailInformationIncomplete(service)) {
            throw new GlobalException(ErrorCode.INCOMPLETE_SERVICE_DETAIL);
        }

        // 조회수 증가
        service.updateViewsUp();
        publicServiceRepository.save(service);

        recentVisitService.addVisit(userId, serviceId);

        PublicServiceDetailResponseDto dto = publicServiceMapper.toDetailDto(service);

        if (userId != 0L) {
            // 로그인한 사용자면 북마크 여부 확인
            dto.setBookmarked(bookmarkRepository.existsByUserIdAndPublicServiceId(userId, service.getId()));
        }

        return dto;
    }

    // 인기 서비스 목록 조회(6개 고정) - 캐싱적용
    @Transactional(readOnly = true)
    public List<PublicServiceListResponseDto> getPopularServices(Long userId) {

        // 북마크 수 기준으로 상위 6개 서비스 조회
        return publicServiceRepository.findTop6ByOrderByBookmarkCntDesc().stream()
            .map(service -> {
                PublicServiceListResponseDto dto = publicServiceMapper.toListDto(service);
                // 로그인한 사용자는 북마크 여부 확인
                if (userId != 0L) {
                    dto.setBookmarked(bookmarkRepository.existsByUserIdAndPublicServiceId(userId, service.getId()));
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    // 공공서비스 전체 목록 조회 (정렬 및 필터링 적용)
    @Transactional(readOnly = true)
    public Page<PublicServiceListResponseDto> getAllServices(
        String sort,
        List<String> specialGroups,
        List<String> familyTypes,
        List<String> categories,
        Pageable pageable,
        Long userId) {

        // 정렬 기준 설정 (기본값: 가나다순)
        Sort sorts = Sort.by(Sort.Order.asc("serviceName"));

        if (sort != null) {
            switch (sort.toLowerCase()) {
                case "bookmark":
                    // 북마크 수 기준 내림차순 정렬, 동일하면 서비스명 오름차순
                    sorts = Sort.by(Sort.Order.desc("bookmarkCnt"), Sort.Order.asc("serviceName"));
                    break;
                case "view":
                    // 조회수 기준 내림차순 정렬, 동일하면 서비스명 오름차순
                    sorts = Sort.by(Sort.Order.desc("views"), Sort.Order.asc("serviceName"));
                    break;
                default:
                    // 기본 가나다순 유지
                    break;
            }
        }

        // 페이지 요청 객체 재생성 (정렬 기준 적용)
        PageRequest pageRequest = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            sorts
        );

        // 필터링 조건에 따른 서비스 조회
        Page<PublicService> services;

        if ((specialGroups != null && !specialGroups.isEmpty()) ||
            (familyTypes != null && !familyTypes.isEmpty()) ||
            (categories != null && !categories.isEmpty())) {

            List<ServiceCategory> categoryEnums = new ArrayList<>();
            if (categories != null) {
                for (String category : categories) {
                    categoryEnums.add(publicServiceValidate.validateServiceCategory(category));
                }
            }

            List<SpecialGroupEnum> specialGroupEnums = new ArrayList<>();
            if (specialGroups != null) {
                for (String group : specialGroups) {
                    // type 값으로 Enum 찾기
                    boolean found = false;
                    for (SpecialGroupEnum enumValue : SpecialGroupEnum.values()) {
                        if (enumValue.getType().equals(group)) {
                            specialGroupEnums.add(enumValue);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new GlobalException(ErrorCode.INVALID_ENUM_CODE);
                    }
                }
            }

            List<FamilyTypeEnum> familyTypeEnums = new ArrayList<>();
            if (familyTypes != null) {
                for (String type : familyTypes) {
                    // type 값으로 Enum 찾기
                    boolean found = false;
                    for (FamilyTypeEnum enumValue : FamilyTypeEnum.values()) {
                        if (enumValue.getType().equals(type)) {
                            familyTypeEnums.add(enumValue);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new GlobalException(ErrorCode.INVALID_ENUM_CODE);
                    }
                }
            }

            services = publicServiceRepository.findWithFilters(
                categoryEnums,
                specialGroupEnums,
                familyTypeEnums,
                pageRequest
            );
        } else {
            services = publicServiceRepository.findAll(pageRequest);
        }

        // DTO 변환 및 북마크 여부 설정
        return services.map(service -> {
            PublicServiceListResponseDto dto = publicServiceMapper.toListDto(service);
            if (userId != 0L) {
                // 로그인한 사용자면 북마크 여부 확인
                dto.setBookmarked(bookmarkRepository.existsByUserIdAndPublicServiceId(userId, service.getId()));
            }
            return dto;
        });
    }

    // 필터 옵션 조회 (캐싱 적용)
    @Transactional(readOnly = true)
    @Cacheable(value = "filterOptions")
    public Map<String, List<FilterOptionDto>> getFilterOptions() {
        Map<String, List<FilterOptionDto>> filterOptions = new HashMap<>();

        // 서비스 분야 (카테고리) 옵션
        List<FilterOptionDto> categoryOptions = Arrays.stream(ServiceCategory.values())
            .map(category -> new FilterOptionDto(category.name(), category.getType()))
            .collect(Collectors.toList());
        filterOptions.put("categories", categoryOptions);

        // 특수 그룹 (가구형태) 옵션
        List<FilterOptionDto> specialGroupOptions = Arrays.stream(SpecialGroupEnum.values())
            .map(group -> new FilterOptionDto(group.name(), group.getType()))
            .collect(Collectors.toList());
        filterOptions.put("specialGroups", specialGroupOptions);

        // 가족 유형 (가구상황) 옵션
        List<FilterOptionDto> familyTypeOptions = Arrays.stream(FamilyTypeEnum.values())
            .map(type -> new FilterOptionDto(type.name(), type.getType()))
            .collect(Collectors.toList());
        filterOptions.put("familyTypes", familyTypeOptions);

        return filterOptions;
    }

    // 필터 옵션 캐시 무효화 - Enum이 변경될 때
    @Transactional
    @CacheEvict(value = "filterOptions", allEntries = true)
    public void refreshFilterOptions() {
    }

    // 내가 북마크한 서비스 목록 조회
    @Transactional(readOnly = true)
    public Page<PublicServiceListResponseDto> getBookmarkedServices(Long userId, Pageable pageable) {
      Page<PublicService> bookmarkedServices = publicServiceRepository.findByBookmarks_User_Id(userId, pageable);

      List<PublicServiceListResponseDto> serviceDtos = bookmarkedServices.getContent().stream()
          .map(service -> {
              PublicServiceListResponseDto dto = publicServiceMapper.toListDto(service);
              dto.setBookmarked(true);
              return dto;
          })
          .collect(Collectors.toList());

      return new PageImpl<>(serviceDtos, pageable, bookmarkedServices.getTotalElements());

    }

}
