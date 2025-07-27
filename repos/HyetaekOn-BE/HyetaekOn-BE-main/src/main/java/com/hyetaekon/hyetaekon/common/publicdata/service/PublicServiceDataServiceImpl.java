package com.hyetaekon.hyetaekon.common.publicdata.service;

import com.hyetaekon.hyetaekon.common.publicdata.dto.PublicServiceConditionsDataDto;
import com.hyetaekon.hyetaekon.common.publicdata.dto.PublicServiceDataDto;
import com.hyetaekon.hyetaekon.common.publicdata.dto.PublicServiceDetailDataDto;
import com.hyetaekon.hyetaekon.common.publicdata.mapper.PublicServiceDataMapper;
import com.hyetaekon.hyetaekon.common.publicdata.mongodb.service.PublicDataMongoService;
import com.hyetaekon.hyetaekon.common.publicdata.util.PublicDataPath;
import com.hyetaekon.hyetaekon.common.publicdata.util.PublicServiceDataValidate;
import com.hyetaekon.hyetaekon.publicservice.entity.*;
import com.hyetaekon.hyetaekon.publicservice.repository.PublicServiceRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestTemplate;


import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class PublicServiceDataServiceImpl implements PublicServiceDataService {
    private final PublicDataMongoService publicDataMongoService;
    private final PublicServiceRepository publicServiceRepository;
    private final PublicServiceDataMapper publicServiceDataMapper;
    private final PublicServiceDataProviderService publicServiceDataProviderService;
    private final RestTemplate restTemplate;
    private final PublicServiceDataValidate validator;

    private static final int DEFAULT_PAGE_SIZE = 1000; // 기본 페이지 크기를 1000으로 설정
    private final Set<String> currentServiceIds = ConcurrentHashMap.newKeySet(); // 현재 동기화된 서비스 ID 저장

    /**
     * 공공서비스 목록 데이터 호출 (페이징 처리)
     */
    public List<PublicServiceDataDto> fetchPublicServiceData(PublicDataPath apiPath, int page, int perPage) {
        return validator.validateAndHandleException(() -> {
            URI uri = publicServiceDataProviderService.createUri(apiPath, "page", String.valueOf(page), "perPage", String.valueOf(perPage));
            ResponseEntity<PublicServiceDataDto> response = restTemplate.getForEntity(uri, PublicServiceDataDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Collections.singletonList(response.getBody());
            } else {
                log.error("공공서비스 목록 조회 실패 (page: {}, perPage: {}): {}", page, perPage, response.getStatusCode());
                return Collections.emptyList();
            }
        }, apiPath);
    }

    /**
     * 공공서비스 상세정보 데이터 호출 (페이징 처리)
     */
    public List<PublicServiceDetailDataDto> fetchPublicServiceDetailData(PublicDataPath apiPath, int page, int perPage) {
        return validator.validateAndHandleException(() -> {
            URI uri = publicServiceDataProviderService.createUri(apiPath, "page", String.valueOf(page), "perPage", String.valueOf(perPage));
            ResponseEntity<PublicServiceDetailDataDto> response = restTemplate.getForEntity(uri, PublicServiceDetailDataDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Collections.singletonList(response.getBody());
            } else {
                log.error("공공서비스 상세정보 조회 실패 (page: {}, perPage: {}): {}", page, perPage, response.getStatusCode());
                return Collections.emptyList();
            }
        }, apiPath);
    }

    /**
     * 공공서비스 지원조건 데이터 호출 (페이징 처리)
     */
    public List<PublicServiceConditionsDataDto> fetchPublicServiceConditionsData(PublicDataPath apiPath, int page, int perPage) {
        return validator.validateAndHandleException(() -> {
            URI uri = publicServiceDataProviderService.createUri(apiPath, "page", String.valueOf(page), "perPage", String.valueOf(perPage));
            ResponseEntity<PublicServiceConditionsDataDto> response = restTemplate.getForEntity(uri, PublicServiceConditionsDataDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Collections.singletonList(response.getBody());
            } else {
                log.error("공공서비스 지원조건 조회 실패 (page: {}, perPage: {}): {}", page, perPage, response.getStatusCode());
                return Collections.emptyList();
            }
        }, apiPath);
    }

    /**
     * 공공서비스 목록 데이터 전체 동기화 (페이징 처리)
     */
    @Transactional
    public void syncPublicServiceData(PublicDataPath apiPath) {
        syncDataWithPaging(
            apiPath,
            // 데이터 조회 함수
            (path, pg) -> fetchPublicServiceData(path, pg, DEFAULT_PAGE_SIZE),
            // 데이터 추출 함수
            PublicServiceDataDto::getData,
            // 전체 개수 getter
            PublicServiceDataDto::getTotalCount,
            // 현재 페이지 개수 getter
            PublicServiceDataDto::getCurrentCount,
            // 데이터 처리 함수
            this::upsertServiceData,
            "공공서비스 목록 데이터"
        );
    }

    /**
     * 공공서비스 상세정보 데이터 전체 동기화 (페이징 처리)
     */
    @Transactional
    public void syncPublicServiceDetailData(PublicDataPath apiPath) {
        syncDataWithPaging(
            apiPath,
            (path, pg) -> fetchPublicServiceDetailData(path, pg, DEFAULT_PAGE_SIZE),
            PublicServiceDetailDataDto::getData,
            PublicServiceDetailDataDto::getTotalCount,
            PublicServiceDetailDataDto::getCurrentCount,
            this::upsertServiceDetailData,
            "공공서비스 상세정보 데이터"
        );
    }

    /**
     * 공공서비스 지원조건 데이터 전체 동기화 (페이징 처리)
     */
    @Transactional
    public void syncPublicServiceConditionsData(PublicDataPath apiPath) {
        syncDataWithPaging(
            apiPath,
            (path, pg) -> fetchPublicServiceConditionsData(path, pg, DEFAULT_PAGE_SIZE),
            PublicServiceConditionsDataDto::getData,
            PublicServiceConditionsDataDto::getTotalCount,
            PublicServiceConditionsDataDto::getCurrentCount,
            this::upsertSupportConditionsData,
            "공공서비스 지원조건 데이터"
        );
    }

    /**
     * 공통 데이터 동기화 메서드 - 중복 코드 제거를 위한 템플릿 메서드
     */
    private <T, D> void syncDataWithPaging(
        PublicDataPath apiPath,
        BiFunction<PublicDataPath, Integer, List<T>> fetcher, // 데이터 조회 함수
        Function<T, List<D>> dataExtractor, // DTO에서 데이터 추출 함수
        Function<T, Long> totalCountGetter, // 전체 개수 조회 함수
        Function<T, Long> currentCountGetter, // 현재 페이지 개수 조회 함수
        Function<List<D>, List<D>> processor, // 데이터 처리 함수
        String operationName) { // 작업 이름 (로깅용)

        currentServiceIds.clear(); // ID 초기화 (필요한 경우)

        int page = 1;
        int perPage = DEFAULT_PAGE_SIZE;
        boolean hasMoreData = true;
        long totalProcessed = 0;

        try {
            while (hasMoreData) {
                // 데이터 조회
                List<T> dtoList = fetcher.apply(apiPath, page);
                List<D> pageData = new ArrayList<>();

                // 페이지 데이터 추출
                for (T dto : dtoList) {
                    List<D> extractedData = dataExtractor.apply(dto);
                    if (extractedData != null && !extractedData.isEmpty()) {
                        pageData.addAll(extractedData);

                        // 페이징 처리 로직
                        long totalCount = totalCountGetter.apply(dto);
                        long currentCount = currentCountGetter.apply(dto);

                        // 더 이상 데이터가 없는지 확인
                        if (currentCount < perPage || (long)page * perPage >= totalCount) {
                            hasMoreData = false;
                        }
                    } else {
                        hasMoreData = false;
                    }
                }

                // 데이터 처리 및 저장
                if (!pageData.isEmpty()) {
                    List<D> processedData = processor.apply(pageData);
                    totalProcessed += processedData.size();
                    log.info("{} 페이지 {} 처리 완료: {}건, 총 {}건",
                        operationName, page, pageData.size(), totalProcessed);
                } else {
                    hasMoreData = false;
                }

                page++;
            }
        } catch (Exception e) {
            log.error("{} 동기화 중 오류 발생", operationName, e);
            throw e;
        }

        log.info("{} 전체 동기화 완료: 총 {}건", operationName, totalProcessed);
    }

    /**
     * 공공서비스 목록 데이터 저장 (배치 처리)
     */
    @Transactional
    @Override
    public List<PublicServiceDataDto.Data> upsertServiceData(List<PublicServiceDataDto.Data> dataList) {
        List<PublicServiceDataDto.Data> validatedData = new ArrayList<>();
        List<PublicService> entitiesToSave = new ArrayList<>();

        for (PublicServiceDataDto.Data data : dataList) {
            // 데이터 유효성 검증
            if (!validator.validatePublicServiceData(data)) {
                continue;
            }

            // 현재 서비스 ID 기록 (미사용 데이터 삭제용)
            currentServiceIds.add(data.getServiceId());

            // 기존 서비스 조회 또는 신규 생성
            PublicService publicService = publicServiceRepository.findById(data.getServiceId())
                .orElse(PublicService.builder().build());

            // 데이터 매핑 및 업데이트
            publicService = publicServiceDataMapper.updateFromServiceData(publicService, data);

            entitiesToSave.add(publicService);
            validatedData.add(data);

            // 배치 처리 최적화: 1000개 단위로 저장
            if (entitiesToSave.size() >= 1000) {
                List<PublicService> savedEntities = publicServiceRepository.saveAll(entitiesToSave);
                publicDataMongoService.updateOrCreateBulkDocuments(savedEntities);
                entitiesToSave.clear();
            }
        }

        // 나머지 데이터 저장
        if (!entitiesToSave.isEmpty()) {
            List<PublicService> savedEntities = publicServiceRepository.saveAll(entitiesToSave);
            publicDataMongoService.updateOrCreateBulkDocuments(savedEntities);
        }

        log.info("공공서비스 목록 데이터 {}건 저장 완료", validatedData.size());
        return validatedData;
    }

    /**
     * 공공서비스 상세정보 데이터 저장 (배치 처리)
     */
    @Transactional
    public List<PublicServiceDetailDataDto.Data> upsertServiceDetailData(List<PublicServiceDetailDataDto.Data> dataList) {
        List<PublicServiceDetailDataDto.Data> validatedData = new ArrayList<>();
        List<PublicService> entitiesToSave = new ArrayList<>();

        // 서비스 ID 목록 생성
        Set<String> serviceIds = dataList.stream()
            .map(PublicServiceDetailDataDto.Data::getServiceId)
            .collect(Collectors.toSet());

        // 서비스 ID로 한 번에 조회 (N+1 문제 방지)
        Map<String, PublicService> serviceMap = publicServiceRepository.findAllById(serviceIds)
            .stream()
            .collect(Collectors.toMap(PublicService::getId, service -> service));

        for (PublicServiceDetailDataDto.Data data : dataList) {
            // 데이터 유효성 검증
            if (!validator.validatePublicServiceDetailData(data)) {
                continue;
            }

            // 기존 서비스 조회
            PublicService publicService = serviceMap.get(data.getServiceId());

            if (publicService != null) {
                // 데이터 매핑 및 업데이트
                publicService = publicServiceDataMapper.updateFromDetailData(publicService, data);
                entitiesToSave.add(publicService);
                validatedData.add(data);
            } else {
                log.warn("⚠️ 공공 서비스 상세내용 ID {}에 해당하는 공공서비스가 존재하지 않습니다.", data.getServiceId());
            }

            // 배치 처리 최적화: 1000개 단위로 저장
            if (entitiesToSave.size() >= 1000) {
                List<PublicService> savedEntities = publicServiceRepository.saveAll(entitiesToSave);
                publicDataMongoService.updateOrCreateBulkDocuments(savedEntities);
                entitiesToSave.clear();
            }
        }

        // 나머지 데이터 저장
        if (!entitiesToSave.isEmpty()) {
            List<PublicService> savedEntities = publicServiceRepository.saveAll(entitiesToSave);
            publicDataMongoService.updateOrCreateBulkDocuments(savedEntities);
        }

        log.info("공공서비스 상세정보 데이터 {}건 저장 완료", validatedData.size());
        return validatedData;
    }

    /**
     * 공공서비스 지원조건 데이터 저장 (배치 처리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<PublicServiceConditionsDataDto.Data> upsertSupportConditionsData(List<PublicServiceConditionsDataDto.Data> dataList) {
        List<PublicServiceConditionsDataDto.Data> validatedData = new ArrayList<>();
        List<PublicService> entitiesToSave = new ArrayList<>();

        // 서비스 ID 목록 생성
        Set<String> serviceIds = dataList.stream()
            .map(PublicServiceConditionsDataDto.Data::getServiceId)
            .collect(Collectors.toSet());

        // 서비스 ID로 한 번에 조회 (N+1 문제 방지)
        Map<String, PublicService> serviceMap = publicServiceRepository.findAllById(serviceIds)
            .stream()
            .collect(Collectors.toMap(PublicService::getId, service -> service));

        for (PublicServiceConditionsDataDto.Data data : dataList) {
            // 데이터 유효성 검증
            if (!validator.validatePublicServiceConditionsData(data)) {
                continue;
            }

            // 기존 서비스 조회
            PublicService publicService = serviceMap.get(data.getServiceId());

            if (publicService != null) {
                // 기본 조건 데이터 업데이트
                publicService = publicServiceDataMapper.updateFromConditionsData(publicService, data);

                // 특수 그룹, 가족 유형, 직업, 사업체 유형 업데이트
                updateRelatedEntities(publicService, data);

                entitiesToSave.add(publicService);
                validatedData.add(data);
            } else {
                log.warn("⚠️ 공공 서비스 지원 ID {}에 해당하는 공공서비스가 존재하지 않습니다.", data.getServiceId());
            }

            // 배치 처리 최적화: 1000개 단위로 저장
            if (entitiesToSave.size() >= 1000) {
                List<PublicService> savedEntities = publicServiceRepository.saveAll(entitiesToSave);
                publicDataMongoService.updateOrCreateBulkDocuments(savedEntities);
                entitiesToSave.clear();
            }
        }

        // 나머지 데이터 저장
        if (!entitiesToSave.isEmpty()) {
            List<PublicService> savedEntities = publicServiceRepository.saveAll(entitiesToSave);
            publicDataMongoService.updateOrCreateBulkDocuments(savedEntities);
        }

        log.info("공공서비스 지원조건 데이터 {}건 저장 완료", validatedData.size());
        return validatedData;
    }


    /**
     * 한 번에 관련 엔티티 업데이트 (성능 최적화)
     */
    private void updateRelatedEntities(PublicService publicService, PublicServiceConditionsDataDto.Data data) {
        // 특수 그룹 업데이트
        publicServiceDataProviderService.updateSpecialGroups(publicService, data);

        // 가족 유형 업데이트
        publicServiceDataProviderService.updateFamilyTypes(publicService, data);

        // 직업 유형 업데이트
        publicServiceDataProviderService.updateOccupations(publicService, data);

        // 사업체 유형 업데이트
        publicServiceDataProviderService.updateBusinessTypes(publicService, data);
    }

    /**
     * 더 이상 사용되지 않는 공공서비스 데이터 정리
     */
    @Transactional
    public int cleanupObsoleteServices() {
        if (currentServiceIds.isEmpty()) {
            log.warn("미사용 데이터 삭제를 위한 현재 서비스 ID 목록이 비어있습니다. 삭제 작업을 건너뜁니다.");
            return 0;
        }

        int deletedCount = publicServiceRepository.deleteByIdNotIn(new ArrayList<>(currentServiceIds));
        log.info("미사용 공공서비스 데이터 {}건 삭제 완료", deletedCount);
        return deletedCount;
    }
}
