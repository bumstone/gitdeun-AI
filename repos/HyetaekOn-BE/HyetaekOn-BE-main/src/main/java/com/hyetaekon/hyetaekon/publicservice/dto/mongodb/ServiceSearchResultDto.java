package com.hyetaekon.hyetaekon.publicservice.dto.mongodb;

import com.hyetaekon.hyetaekon.publicservice.entity.mongodb.ServiceInfo;
import lombok.AllArgsConstructor;
import lombok.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ServiceSearchResultDto {
    private final List<ServiceInfo> results;
    private final long total;
    private final int currentPage;
    private final int totalPages;
    private final boolean hasNext;

    public static ServiceSearchResultDto of(List<ServiceInfo> results, long total, Pageable pageable) {
        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());
        return ServiceSearchResultDto.builder()
            .results(results)
            .total(total)
            .currentPage(pageable.getPageNumber())
            .totalPages(totalPages)
            .hasNext(pageable.getPageNumber() + 1 < totalPages)
            .build();
    }
}
