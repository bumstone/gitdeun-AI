package com.hyetaekon.hyetaekon.publicservice.repository.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.hyetaekon.hyetaekon.publicservice.dto.mongodb.ServiceSearchCriteriaDto;
import com.hyetaekon.hyetaekon.publicservice.dto.mongodb.ServiceSearchResultDto;
import com.hyetaekon.hyetaekon.publicservice.entity.mongodb.ServiceInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceSearchClient {
    private final MongoTemplate mongoTemplate;
    private static final String SEARCH_INDEX = "searchIndex";
    private static final String AUTOCOMPLETE_INDEX = "serviceAutocompleteIndex";
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
                score: {$meta: 'searchScore'}
            }
        }""";

    public ServiceSearchResultDto search(ServiceSearchCriteriaDto criteria) {
        List<AggregationOperation> operations = new ArrayList<>();

        // 검색 쿼리 추가
        operations.add(context -> Document.parse(buildSearchQuery(criteria)));

        // 프로젝션 추가
        operations.add(context -> Document.parse(PROJECT_STAGE));

        // 페이징 처리
        operations.add(context -> Document.parse(buildFacetStage(criteria.getPageable())));

        AggregationResults<Document> results = mongoTemplate.aggregate(
            Aggregation.newAggregation(operations),
            COLLECTION_NAME,
            Document.class
        );

        return processResults(results, criteria.getPageable());
    }

    private String buildSearchQuery(ServiceSearchCriteriaDto criteria) {
        List<String> shouldClauses = new ArrayList<>();

        // 검색어 관련 조건 추가
        if (StringUtils.hasText(criteria.getSearchTerm())) {
            addSearchTermClauses(shouldClauses, criteria.getSearchTerm());
        }

        // 사용자 관심사 관련 조건 추가
        if (criteria.getUserInterests() != null && !criteria.getUserInterests().isEmpty()) {
            for (String interest : criteria.getUserInterests()) {
                shouldClauses.add(createSearchClause("serviceCategory", interest, 3.5f, 0));
                shouldClauses.add(createSearchClause("specialGroup", interest, 3.5f, 0));
                shouldClauses.add(createSearchClause("familyType", interest, 3.5f, 0));
            }
        }

        // 사용자 정보(성별, 나이, 직업, 소득) 기반 조건 추가
        addUserMatchBoosts(shouldClauses, criteria);

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
        }""".formatted(SEARCH_INDEX, compoundQuery);
    }

    private void addSearchTermClauses(List<String> clauses, String searchTerm) {
        // 서비스명 검색
        clauses.add(createSearchClause("serviceName", searchTerm, 5.0f, 1));
        clauses.add(createSearchClause("serviceName", searchTerm, 4.5f, 2));

        // 요약 검색
        clauses.add(createSearchClause("summaryPurpose", searchTerm, 3.5f, 0));

        // 서비스 분야 검색
        clauses.add(createSearchClause("serviceCategory", searchTerm, 4.5f, 1));

        // 특수그룹 검색
        clauses.add(createSearchClause("specialGroup", searchTerm, 4.0f, 1));

        // 가족유형 검색
        clauses.add(createSearchClause("familyType", searchTerm, 4.0f, 1));

        // 직업 검색
        clauses.add(createSearchClause("occupations", searchTerm, 3.0f, 0));

        // 사업자 유형 검색
        clauses.add(createSearchClause("businessTypes", searchTerm, 3.0f, 0));

        // 정규식 전방 일치 검색 (서비스명)
        clauses.add("""
            {regex: {
                query: '%s.*',
                path: 'serviceName',
                allowAnalyzedField: true,
                score: {boost: {value: 4.0}}
            }}""".formatted(searchTerm));
    }

    private void addUserMatchBoosts(List<String> clauses, ServiceSearchCriteriaDto criteria) {
        // 성별 조건 (should로 변경)
        if (StringUtils.hasText(criteria.getUserGender())) {
            String genderField = "MALE".equalsIgnoreCase(criteria.getUserGender())
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
                    score: {boost: {value: 5.0}}
                }
            }
            """.formatted(genderField, genderField));
        }

        // 나이 조건 (should로 변경)
        if (criteria.getUserAge() != null) {
            int age = criteria.getUserAge();

            // 대상 나이 범위가 null이거나 사용자 나이를 포함하는 서비스에 가산점
            clauses.add("""
            {
                compound: {
                    should: [
                        {
                            compound: {
                                mustNot: [{exists: {path: "targetAgeStart"}}]
                            }
                        },
                        {
                            range: {path: "targetAgeStart", lte: %d}
                        }
                    ],
                    score: {boost: {value: 4.5}}
                }
            }
            """.formatted(age));

            clauses.add("""
            {
                compound: {
                    should: [
                        {
                            compound: {
                                mustNot: [{exists: {path: "targetAgeEnd"}}]
                            }
                        },
                        {
                            range: {path: "targetAgeEnd", gte: %d}
                        }
                    ],
                    score: {boost: {value: 4.5}}
                }
            }
            """.formatted(age));
        }

        // 직업 일치 가산점
        if (StringUtils.hasText(criteria.getUserJob())) {
            String userJob = criteria.getUserJob();

            // Occupation 필드와 일치 시 가산점
            clauses.add(createSearchClause("occupations", userJob, 3.0f, 0));

            // BusinessType 필드와 일치 시 가산점
            clauses.add(createSearchClause("businessTypes", userJob, 3.0f, 0));
        }

        // 소득 수준 일치 가산점
        if (StringUtils.hasText(criteria.getUserIncomeLevel())) {
            String userIncomeLevel = criteria.getUserIncomeLevel();

            // 1. 정확히 일치하는 소득수준에 높은 가산점
            clauses.add("""
            {text: {
                query: '%s',
                path: 'incomeLevel',
                score: {boost: {value: 2.8}}
            }}""".formatted(userIncomeLevel));

            // 2. ANY 값은 모든 소득수준에 매칭 가능
            clauses.add("""
            {text: {
                query: 'ANY',
                path: 'incomeLevel',
                score: {boost: {value: 1.0}}
            }}""");

            // 3. 사용자 소득수준보다 낮은 범위도 포함 (범위별 가산점)
            addIncomeLevelRangeBoosts(clauses, userIncomeLevel);
        }
    }

    // 소득수준 범위에 따른 가산점 추가
    private void addIncomeLevelRangeBoosts(List<String> clauses, String userIncomeLevel) {
        switch (userIncomeLevel) {
            case "HIGH":
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_HIGH", 2.5f, 0));
                clauses.add(createSearchClause("incomeLevel", "MIDDLE", 2.0f, 0));
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_LOW", 1.5f, 0));
                clauses.add(createSearchClause("incomeLevel", "LOW", 1.0f, 0));
                break;
            case "MIDDLE_HIGH":
                clauses.add(createSearchClause("incomeLevel", "MIDDLE", 2.5f, 0));
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_LOW", 2.0f, 0));
                clauses.add(createSearchClause("incomeLevel", "LOW", 1.5f, 0));
                break;
            case "MIDDLE":
                clauses.add(createSearchClause("incomeLevel", "MIDDLE_LOW", 2.0f, 0));
                clauses.add(createSearchClause("incomeLevel", "LOW", 1.5f, 0));
                break;
            case "MIDDLE_LOW":
                clauses.add(createSearchClause("incomeLevel", "LOW", 2.0f, 0));
                break;
            default:
                break;
        }
    }

    private String createSearchClause(String path, String query, float boost, int maxEdits) {
        return """
            {text: {
                query: '%s',
                path: '%s',
                score: {boost: {value: %.1f}}%s
            }}""".formatted(
            query,
            path,
            boost,
            maxEdits > 0 ? ", fuzzy: {maxEdits: " + maxEdits + "}" : ""
        );
    }

    private String buildFacetStage(Pageable pageable) {
        return """
            {
                $facet: {
                    results: [{$skip: %d}, {$limit: %d}],
                    total: [{$count: 'count'}]
                }
            }""".formatted(pageable.getOffset(), pageable.getPageSize());
    }

    private ServiceSearchResultDto processResults(AggregationResults<Document> results, Pageable pageable) {
        Document result = results.getUniqueMappedResult();
        if (result == null) {
            return ServiceSearchResultDto.of(List.of(), 0L, pageable);
        }

        List<Document> resultDocs = result.get("results", List.class);
        List<Document> totalDocs = result.get("total", List.class);

        if (resultDocs == null) {
            return ServiceSearchResultDto.of(List.of(), 0L, pageable);
        }

        List<ServiceInfo> searchResults = resultDocs.stream()
            .map(doc -> mongoTemplate.getConverter().read(ServiceInfo.class, doc))
            .toList();

        long total = 0L;
        if (totalDocs != null && !totalDocs.isEmpty()) {
            Number count = totalDocs.getFirst().get("count", Number.class);
            total = count != null ? count.longValue() : 0L;
        }

        // 중복 제거: publicServiceId가 같은 경우 하나만 유지
        Map<String, ServiceInfo> uniqueResults = new LinkedHashMap<>();
        for (ServiceInfo info : searchResults) {
            uniqueResults.putIfAbsent(info.getPublicServiceId(), info);
        }

        List<ServiceInfo> dedupedResults = new ArrayList<>(uniqueResults.values());

        // 중복 제거 로깅
        int removedDuplicates = searchResults.size() - dedupedResults.size();
        if (removedDuplicates > 0) {
            log.warn("검색 결과에서 중복된 항목 {}개가 제거되었습니다.", removedDuplicates);
        }

        return ServiceSearchResultDto.of(dedupedResults, total, pageable);
    }

    // 검색어 자동완성
    public List<String> getAutocompleteResults(String word) {
        if (!StringUtils.hasText(word) || word.length() < 2) {
            return new ArrayList<>();
        }

        return mongoTemplate.aggregate(
                Aggregation.newAggregation(
                    context -> Document.parse("""
                    {
                        $search: {
                            index: '%s',
                            autocomplete: {
                                query: '%s',
                                path: 'serviceName',
                                fuzzy: {maxEdits: 1}
                            }
                        }
                    }""".formatted(AUTOCOMPLETE_INDEX, word)),
                    Aggregation.project("serviceName"),
                    Aggregation.limit(8)
                ),
                COLLECTION_NAME,
                Document.class
            )
            .getMappedResults()
            .stream()
            .map(doc -> doc.getString("serviceName"))
            .distinct()
            .collect(Collectors.toList());
    }
}
