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
public class PublicServiceDetailDataDto {
    private List<Data> data;
    private long totalCount;
    private long currentCount;
    private long matchCount;
    private long page;
    private long perPage;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Data {
        @JsonProperty("서비스ID")
        private String serviceId;

        @JsonProperty("서비스명")
        private String serviceName;

        @JsonProperty("서비스목적")
        private String servicePurpose;

        @JsonProperty("지원대상")
        private String supportTarget;

        @JsonProperty("선정기준")
        private String selectionCriteria;

        @JsonProperty("지원내용")
        private String supportDetail;

        @JsonProperty("지원유형")
        private String supportType;

        @JsonProperty("신청방법")
        private String applicationMethod;

        @JsonProperty("신청기한")
        private String applicationDeadline;

        /*@JsonProperty("구비서류")
        private String requiredDocuments;*/

        @JsonProperty("문의처")
        private String contactInfo;

        @JsonProperty("온라인신청사이트URL")
        private String onlineApplicationUrl;

        @JsonProperty("소관기관명")
        private String governingAgency;

        /*@JsonProperty("법령")
        private String relatedLaws;*/
    }
}
