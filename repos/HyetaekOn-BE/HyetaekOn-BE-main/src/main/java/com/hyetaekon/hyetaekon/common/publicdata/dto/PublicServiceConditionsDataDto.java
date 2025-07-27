package com.hyetaekon.hyetaekon.common.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicServiceConditionsDataDto {
    private List<Data> data;
    private long totalCount;
    private long currentCount;
    private long matchCount;
    private long page;
    private long perPage;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Data {
        @JsonProperty("서비스ID")
        private String serviceId;

        @JsonProperty("JA0101")
        private String targetGenderMale;
        @JsonProperty("JA0102")
        private String targetGenderFemale;

        @JsonProperty("JA0110")
        private Integer targetAgeStart;
        @JsonProperty("JA0111")
        private Integer targetAgeEnd;

        // 소득 수준
        @JsonProperty("JA0201")
        private String incomeLevelVeryLow; // 중위소득 0~50%
        @JsonProperty("JA0202")
        private String incomeLevelLow; // 중위소득 51~75%
        @JsonProperty("JA0203")
        private String incomeLevelMedium; // 중위소득 76~100%
        @JsonProperty("JA0204")
        private String incomeLevelHigh; // 중위소득 101~200%
        @JsonProperty("JA0205")
        private String incomeLevelVeryHigh; // 중위소득 200% 초과

        // Special Group
        @JsonProperty("JA0401")
        private String JA0401;  // 다문화가족
        @JsonProperty("JA0402")
        private String JA0402;  // 북한이탈주민
        @JsonProperty("JA0403")
        private String JA0403;  // 한부모가정/조손가정
        @JsonProperty("JA0404")
        private String JA0404;  // 1인가구
        @JsonProperty("JA0328")
        private String JA0328;  // 장애인
        @JsonProperty("JA0329")
        private String JA0329;  // 국가보훈대상자
        @JsonProperty("JA0330")
        private String JA0330;  // 질병/질환자

        // Family Type
        @JsonProperty("JA0411")
        private String JA0411;  // 다자녀가구
        @JsonProperty("JA0412")
        private String JA0412;  // 무주택세대
        @JsonProperty("JA0413")
        private String JA0413;  // 신규전입
        @JsonProperty("JA0414")
        private String JA0414;  // 확대가족

        // Occupation
        @JsonProperty("JA0313")
        private String JA0313;  // 농업인
        @JsonProperty("JA0314")
        private String JA0314;  // 어업인
        @JsonProperty("JA0315")
        private String JA0315;  // 축산업인
        @JsonProperty("JA0316")
        private String JA0316;  // 임업인
        @JsonProperty("JA0317")
        private String JA0317;  // 초등학생
        @JsonProperty("JA0318")
        private String JA0318;  // 중학생
        @JsonProperty("JA0319")
        private String JA0319;  // 고등학생
        @JsonProperty("JA0320")
        private String JA0320;  // 대학생/대학원생
        @JsonProperty("JA0326")
        private String JA0326;  // 근로자/직장인
        @JsonProperty("JA0327")
        private String JA0327;  // 구직자/실업자

        // Business Type
        @JsonProperty("JA1101")
        private String JA1101;  // 예비 창업자
        @JsonProperty("JA1102")
        private String JA1102;  // 영업중
        @JsonProperty("JA1103")
        private String JA1103;  // 생계곤란/폐업예정자
        @JsonProperty("JA1201")
        private String JA1201;  // 음식업
        @JsonProperty("JA1202")
        private String JA1202;  // 제조업
        @JsonProperty("JA1299")
        private String JA1299;  // 기타업종
        @JsonProperty("JA2101")
        private String JA2101;  // 중소기업
        @JsonProperty("JA2102")
        private String JA2102;  // 사회복지시설
        @JsonProperty("JA2103")
        private String JA2103;  // 기관/단체
        @JsonProperty("JA2201")
        private String JA2201;  // 제조업
        @JsonProperty("JA2202")
        private String JA2202;  // 농업, 임업 및 어업
        @JsonProperty("JA2203")
        private String JA2203;  // 정보통신업
        @JsonProperty("JA2299")
        private String JA2299;  // 기타업종

    }
}