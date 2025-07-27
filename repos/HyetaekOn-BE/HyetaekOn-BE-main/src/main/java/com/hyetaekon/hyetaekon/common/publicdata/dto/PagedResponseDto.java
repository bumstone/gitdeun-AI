package com.hyetaekon.hyetaekon.common.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공통 페이징 응답 DTO
 * 임시
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponseDto<T> {

    @JsonProperty("currentCount")
    private long currentCount;  // 현재 페이지에서 반환된 데이터 개수

    @JsonProperty("matchCount")
    private long matchCount;    // 전체 검색된 데이터 개수

    @JsonProperty("page")
    private long page;          // 현재 페이지 번호

    @JsonProperty("perPage")
    private long perPage;       // 페이지당 데이터 개수

    @JsonProperty("totalCount")
    private long totalCount;    // 전체 데이터 개수

    @JsonProperty("data")
    private List<T> data;       // 실제 데이터 리스트
}
