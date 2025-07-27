package com.hyetaekon.hyetaekon.publicservice.repository.mongodb;

import com.hyetaekon.hyetaekon.publicservice.dto.mongodb.ServiceSearchResultDto;
import com.hyetaekon.hyetaekon.publicservice.entity.mongodb.ServiceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchedServiceClient {
    private final MongoTemplate mongoTemplate;
    private static final String INDEX_NAME = "searchIndex";
    private static final String COLLECTION_NAME = "service_info";

    private static final String PROJECT_STAGE = """
        {
            $project: {
                publicServiceId: 1,
                serviceName: 1,
                summaryPurpose: 1,
                serviceCategory: 1,
                specialGroup: 1,
                familyType: 1,
                occupations: 1,
                businessTypes: 1,
                targetGenderMale: 1,
                targetGenderFemale: 1,
                targetAgeStart: 1,
                targetAgeEnd: 1,
                incomeLevel: 1,
                matchCount: 1, 
                score: {$meta: 'searchScore'}
            }
        }""";

    /**
     * 사용자 맞춤 공공서비스 추천
     */
    public ServiceSearchResultDto getMatchedServices(
        List<String> keywords,
        String userGender,
        Integer userAge,
        String userIncomeLevel,
        String userJob,
        int size) {

        // 키워드가 없는 경우 빈 결과 반환
        if (keywords == null || keywords.isEmpty()) {
            return ServiceSearchResultDto.of(Collections.emptyList(),0L, PageRequest.of(0, size));
        }

        String searchQuery = buildSearchQuery(
            keywords,
            userGender,
            userAge,
            userIncomeLevel,
            userJob
        );

        // 매칭된 키워드 수를 계산하는 스테이지
        String matchCountStage = buildMatchCountStage(keywords);

        // 매칭된 키워드가 있는 것만 필터링
        String filterStage = "{$match: {matchCount: {$gt: 0}}}";

        // 매칭 수와 검색 점수로 정렬
        String sortStage = "{$sort: {matchCount: -1, score: -1}}";

        // 결과 제한
        String limitStage = String.format("{$limit: %d}", size);

        AggregationOperation searchOperation = context -> Document.parse(searchQuery);
        AggregationOperation matchCountOperation = context -> Document.parse(matchCountStage);
        AggregationOperation filterOperation = context -> Document.parse(filterStage);
        AggregationOperation sortOperation = context -> Document.parse(sortStage);
        AggregationOperation projectOperation = context -> Document.parse(PROJECT_STAGE);
        AggregationOperation limitOperation = context -> Document.parse(limitStage);

        AggregationResults<Document> results = mongoTemplate.aggregate(
            Aggregation.newAggregation(
                searchOperation,
                matchCountOperation,
                filterOperation,
                sortOperation,
                projectOperation,
                limitOperation
            ),
            COLLECTION_NAME,
            Document.class
        );

        return processResults(results, size);
    }

    /**
     * 검색 쿼리 생성
     */
    private String buildSearchQuery(
        List<String> keywords,
        String userGender,
        Integer userAge,
        String userIncomeLevel,
        String userJob) {

        // should 조건 (가중치가 적용된 키워드 조건)
        List<String> shouldClauses = createKeywordMatchClauses(keywords);

        // 성별과 나이 조건도 should로 변경
        addUserMatchBoosts(shouldClauses, userGender, userAge, userIncomeLevel, userJob);

        String shouldClausesStr = shouldClauses.isEmpty() ? "[]" : "[" + String.join(",", shouldClauses) + "]";

        // compound 쿼리 구성 (should 조건만 사용)
        String compoundQuery = """
            compound: {
                should: %s,
                minimumShouldMatch: 1
            }
        """.formatted(shouldClausesStr);

        return """
            {
                $search: {
                    index: '%s',
                    %s
                }
            }""".formatted(INDEX_NAME, compoundQuery);
    }

    /**
     * 키워드 기반 검색 조건 생성
     */
    private List<String> createKeywordMatchClauses(List<String> keywords) {
        List<String> clauses = new ArrayList<>();

        // 모든 키워드에 동일한 가중치 부여
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword)) {
                // 서비스명 검색
                clauses.add(createSearchClause("serviceName", keyword, 3.5f));
                // 요약 검색
                clauses.add(createSearchClause("summaryPurpose", keyword, 3.5f));
                // 서비스 분야 검색
                clauses.add(createSearchClause("serviceCategory", keyword, 5.0f));
                // 특수그룹 검색
                clauses.add(createSearchClause("specialGroup", keyword, 5.0f));
                // 가족유형 검색
                clauses.add(createSearchClause("familyType", keyword, 5.0f));
            }
        }

        return clauses;
    }

    /**
     * 사용자 정보 기반 가산점 추가
     */
    private void addUserMatchBoosts(
        List<String> clauses,
        String userGender,
        Integer userAge,
        String userIncomeLevel,
        String userJob) {

        // 성별 조건 (should로 변경)
        if (StringUtils.hasText(userGender)) {
            String genderField = "MALE".equalsIgnoreCase(userGender)
                ? "targetGenderMale" : "targetGenderFemale";

            // 대상 성별이 null이거나 Y인 서비스에 가산점
            clauses.add("""
            {
                compound: {
                    should: [
                        {
                            compound: {
                                mustNot: [{exists: {path: "%s"}}]
                            }
                        },
                        {
                            equals: {path: "%s", value: "Y"}
                        }
                    ],
                    score: {boost: {value: 5.5}}
                }
            }
            """.formatted(genderField, genderField));
        }

        // 나이 조건 (should로 변경)
        if (userAge != null) {
            clauses.add("""
            {
                range: {
                    path: ["targetAgeStart", "targetAgeEnd"],
                    gte: 0,
                    lte: %d,
                    score: { boost: { value: 4.5 } }
                }
            }
            """.formatted(userAge));
        }

        // 직업 관련 조건 추가
        if (StringUtils.hasText(userJob)) {
            clauses.add(createSearchClause("occupations", userJob, 3.0f));
            clauses.add(createSearchClause("businessTypes", userJob, 3.0f));
        }

        // 소득 수준 관련 조건 추가
        if (StringUtils.hasText(userIncomeLevel)) {
            clauses.add(createSearchClause("incomeLevel", userIncomeLevel, 2.8f));
            clauses.add(createSearchClause("incomeLevel", "ANY", 1.0f));
            // 소득 수준 범위에 따른 가중치 추가
            addIncomeLevelRangeBoosts(clauses, userIncomeLevel);
        }
    }

    /**
     * 검색 조건 생성 헬퍼 메서드
     */
    private String createSearchClause(String path, String query, float boost) {
        return """
            {text: {
                query: '%s',
                path: '%s',
                score: {boost: {value: %.1f}}
            }}""".formatted(query, path, boost);
    }

    /**
     * 소득수준 범위에 따른 가중치 부여
     */
    private void addIncomeLevelRangeBoosts(List<String> clauses, String userIncomeLevel) {
        switch (userIncomeLevel) {
            case "HIGH":
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_HIGH", 2.5f));
                clauses.add(createSearchClause("incomeLevel", "MIDDLE", 2.0f));
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_LOW", 1.5f));
                clauses.add(createSearchClause("incomeLevel", "LOW", 1.0f));
                break;
            case "MIDDLE_HIGH":
                clauses.add(createSearchClause("incomeLevel", "MIDDLE", 2.5f));
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_LOW", 2.0f));
                clauses.add(createSearchClause("incomeLevel", "LOW", 1.5f));
                break;
            case "MIDDLE":
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_LOW", 2.0f));
                clauses.add(createSearchClause("incomeLevel", "LOW", 1.5f));
                break;
            case "MIDDLE_LOW":
                clauses.add(createSearchClause("incomeLevel", "LOW", 2.0f));
                break;
            default:
                break;
        }
    }

    /**
     * 매칭된 키워드 수 계산
     */
    private String buildMatchCountStage(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "{$addFields: {matchCount: 0}}";
        }

        String keywordArray = keywords.stream()
            .filter(StringUtils::hasText)
            .map(keyword -> "\"" + keyword + "\"")
            .collect(Collectors.joining(", "));

        return """
            {$addFields: {
                matchCount: {
                    $add: [
                        {$size: {$ifNull: [{$setIntersection: ["$specialGroup", [%s]]}, []]}},
                        {$size: {$ifNull: [{$setIntersection: ["$familyType", [%s]]}, []]}},
                        {$cond: [{$in: ["$serviceCategory", [%s]]}, 1, 0]}
                    ]
                }
            }}
        """.formatted(keywordArray, keywordArray, keywordArray);
    }

    /**
     * 추천 결과 처리
     */
    private ServiceSearchResultDto processResults(AggregationResults<Document> results, int size) {
        List<Document> resultDocs = results.getMappedResults();
        if (resultDocs.isEmpty()) {
            return ServiceSearchResultDto.of(Collections.emptyList(),0L, PageRequest.of(0, size));
        }

        List<ServiceInfo> searchResults = resultDocs.stream()
            .map(doc -> mongoTemplate.getConverter().read(ServiceInfo.class, doc))
            .collect(Collectors.toList());

        return ServiceSearchResultDto.of(
            searchResults,
            searchResults.size(),
            PageRequest.of(0, size)
        );
    }
}