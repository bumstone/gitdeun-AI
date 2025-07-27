package com.hyetaekon.hyetaekon.publicservice.dto.mongodb;

import lombok.Getter;
import lombok.Builder;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@Getter
@Builder
public class ServiceSearchCriteriaDto {
    private final String searchTerm;                // 검색어
    private final List<String> userInterests;       // 사용자 관심사
    private final String userGender;                // 사용자 성별
    private final Integer userAge;                  // 사용자 나이
    private final String userJob;                   // 사용자 직종
    private final String userIncomeLevel;           // 사용자 소득수준
    private final Pageable pageable;

    // 사용자 정보 추가 메서드
    public ServiceSearchCriteriaDto withUserInfo(
        List<String> userInterests,
        String userGender,
        Integer userAge,
        String userIncomeLevel,
        String userJob) {
        return ServiceSearchCriteriaDto.builder()
            .searchTerm(this.searchTerm)
            .userInterests(userInterests)
            .userGender(userGender)
            .userAge(userAge)
            .userIncomeLevel(userIncomeLevel)
            .userJob(userJob)
            .pageable(this.pageable)
            .build();
    }

}
