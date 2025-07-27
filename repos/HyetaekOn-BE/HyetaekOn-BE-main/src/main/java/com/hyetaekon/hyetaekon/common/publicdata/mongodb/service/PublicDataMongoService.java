package com.hyetaekon.hyetaekon.common.publicdata.mongodb.service;

import com.hyetaekon.hyetaekon.common.publicdata.mongodb.document.PublicData;
import com.hyetaekon.hyetaekon.common.publicdata.mongodb.repository.PublicDataMongoRepository;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataMongoService {

    private final PublicDataMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * 단일 공공서비스 엔티티를 MongoDB에 저장
     */
    public PublicData saveToMongo(PublicService publicService) {
        PublicData document = convertToDocument(publicService);
        return mongoRepository.save(document);
    }

    /**
     * 공공서비스 엔티티를 MongoDB 문서로 변환
     */
    private PublicData convertToDocument(PublicService publicService) {
        // 특수 그룹 정보 추출
        List<String> specialGroups = publicService.getSpecialGroups().stream()
            .map(sg -> sg.getSpecialGroupEnum().getType())
            .collect(Collectors.toList());

        // 가족 유형 정보 추출
        List<String> familyTypes = publicService.getFamilyTypes().stream()
            .map(ft -> ft.getFamilyTypeEnum().getType())
            .collect(Collectors.toList());

        // 직업 정보 추출
        List<String> occupations = publicService.getOccupations().stream()
            .map(occ -> occ.getOccupationEnum().getType())
            .collect(Collectors.toList());

        // 사업체 유형 정보 추출
        List<String> businessTypes = publicService.getBusinessTypes().stream()
            .map(bt -> bt.getBusinessTypeEnum().getType())
            .collect(Collectors.toList());

        // MongoDB 문서 생성 및 반환
        return PublicData.builder()
            .publicServiceId(publicService.getId())
            .serviceName(publicService.getServiceName())
            .summaryPurpose(publicService.getSummaryPurpose())
            .serviceCategory(publicService.getServiceCategory().getType())
            .specialGroup(specialGroups)
            .familyType(familyTypes)
            .occupations(occupations)
            .businessTypes(businessTypes)
            .targetGenderMale(publicService.getTargetGenderMale())
            .targetGenderFemale(publicService.getTargetGenderFemale())
            .targetAgeStart(publicService.getTargetAgeStart())
            .targetAgeEnd(publicService.getTargetAgeEnd())
            .incomeLevel(publicService.getIncomeLevel())
            .build();
    }

    /**
     * 서비스 ID로 문서 조회
     */
    public Optional<PublicData> findByPublicServiceId(String publicServiceId) {
        return mongoRepository.findByPublicServiceId(publicServiceId);
    }

    /**
     * 기존 문서 업데이트 또는 새 문서 생성
     */
    public PublicData updateOrCreateDocument(PublicService publicService) {
        Optional<PublicData> existingDoc = mongoRepository.findByPublicServiceId(publicService.getId());

        if (existingDoc.isPresent()) {
            // 기존 문서의 ID 유지하면서 데이터 업데이트
            PublicData newData = convertToDocument(publicService);
            newData.setId(existingDoc.get().getId());
            return mongoRepository.save(newData);
        } else {
            // 새 문서 생성
            return saveToMongo(publicService);
        }
    }

    /**
     * 여러 서비스 문서 업데이트 또는 생성
     */
    public void updateOrCreateBulkDocuments(List<PublicService> services) {
        // 모든 service ID 목록
        List<String> serviceIds = services.stream()
            .map(PublicService::getId)
            .collect(Collectors.toList());

        // publicServiceId로 기존 문서 조회 (중요: findAllByPublicServiceId를 사용)
        Map<String, PublicData> existingDocsMap = mongoRepository.findAllByPublicServiceIdIn(serviceIds).stream()
            .collect(Collectors.toMap(
                PublicData::getPublicServiceId,
                doc -> doc,
                (a, b) -> a  // 중복 시 첫 번째 문서 유지
            ));

        // 처리할 문서 준비
        List<PublicData> docsToSave = services.stream()
            .map(service -> {
                PublicData doc = convertToDocument(service);
                if (existingDocsMap.containsKey(service.getId())) {
                    // 기존 문서의 ID 유지
                    doc.setId(existingDocsMap.get(service.getId()).getId());
                }
                return doc;
            })
            .collect(Collectors.toList());

        // 저장
        mongoRepository.saveAll(docsToSave);
    }

    // 첫 실행 시에만 중복 제거 및 인덱스 생성
    @PostConstruct
    public void ensureIndexes() {
        try {
            // 1. 기존 인덱스 확인
            boolean hasUniqueIndex = false;
            for (IndexInfo indexInfo : mongoTemplate.indexOps("service_info").getIndexInfo()) {
                if ("publicServiceId_1".equals(indexInfo.getName())) {
                    hasUniqueIndex = indexInfo.isUnique();
                    break;
                }
            }

            // 2. 유니크 인덱스가 없는 경우만 처리
            if (!hasUniqueIndex) {
                // 2.1 일반 인덱스 존재 여부 확인
                boolean hasNonUniqueIndex = false;
                for (IndexInfo indexInfo : mongoTemplate.indexOps("service_info").getIndexInfo()) {
                    if ("publicServiceId_1".equals(indexInfo.getName()) && !indexInfo.isUnique()) {
                        hasNonUniqueIndex = true;
                        break;
                    }
                }

                // 2.2 일반 인덱스가 있다면 삭제
                if (hasNonUniqueIndex) {
                    mongoTemplate.indexOps("service_info").dropIndex("publicServiceId_1");
                    log.info("기존 비유니크 인덱스 삭제: publicServiceId_1");
                }

                // 2.3 최적화된 중복 제거 실행
                deduplicateMongoDocuments();

                // 2.4 유니크 인덱스 생성
                mongoTemplate.indexOps("service_info").ensureIndex(
                    new Index().on("publicServiceId", Sort.Direction.ASC).unique()
                );
                log.info("MongoDB 인덱스 설정 완료: publicServiceId (unique)");
            } else {
                log.info("MongoDB 유니크 인덱스 이미 존재함: publicServiceId_1");
            }
        } catch (Exception e) {
            log.error("MongoDB 인덱스 설정 중 오류 발생: {}", e.getMessage());
            // 인덱스 생성 실패해도 애플리케이션은 시작되도록 함
        }
    }

    @Transactional
    public void deduplicateMongoDocuments() {
        log.info("MongoDB 문서 중복 제거 시작 (최적화 버전)");

        // 모든 publicServiceId와 해당 문서 ID를 그룹화하여 한 번에 조회
        AggregationResults<Document> results = mongoTemplate.aggregate(
            Aggregation.newAggregation(
                Aggregation.group("publicServiceId")
                    .first("_id").as("firstId")
                    .push("_id").as("allIds")
                    .count().as("count")
            ),
            "service_info",
            Document.class
        );

        int totalProcessed = 0;
        int totalRemoved = 0;

        for (Document doc : results.getMappedResults()) {
            int count = doc.getInteger("count");

            // 중복이 있는 경우에만 처리
            if (count > 1) {
                String publicServiceId = doc.getString("_id");
                List<Object> allIds = (List<Object>) doc.get("allIds");
                Object firstId = doc.get("firstId");

                // 첫 번째 문서를 제외한 나머지 문서 삭제
                for (int i = 0; i < allIds.size(); i++) {
                    Object currentId = allIds.get(i);
                    if (!currentId.equals(firstId)) {
                        mongoTemplate.remove(Query.query(Criteria.where("_id").is(currentId)), "service_info");
                        totalRemoved++;
                    }
                }
            }

            totalProcessed++;
            if (totalProcessed % 1000 == 0) {
                log.info("중복 제거 진행 중: {}/{} 그룹 처리, {}개 제거됨",
                    totalProcessed, results.getMappedResults().size(), totalRemoved);
            }
        }

        log.info("MongoDB 문서 중복 제거 완료: 총 {}개 그룹 중 {}개 중복 제거됨",
            totalProcessed, totalRemoved);
    }
}