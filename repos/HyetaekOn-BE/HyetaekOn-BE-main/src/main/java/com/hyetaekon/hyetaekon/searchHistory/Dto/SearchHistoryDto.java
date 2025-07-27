package com.hyetaekon.hyetaekon.searchHistory.Dto;

import com.hyetaekon.hyetaekon.searchHistory.entity.SearchHistory;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryDto {
    private String id;
    private String searchTerm;
    private String createdAt;

    // Entity -> DTO 변환
    public static SearchHistoryDto from(SearchHistory entity) {
        return SearchHistoryDto.builder()
            .id(entity.getId())
            .searchTerm(entity.getSearchTerm())
            .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .build();
    }
}
