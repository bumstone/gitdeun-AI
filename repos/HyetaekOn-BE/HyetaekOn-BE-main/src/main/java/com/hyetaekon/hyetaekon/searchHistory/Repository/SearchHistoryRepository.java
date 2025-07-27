package com.hyetaekon.hyetaekon.searchHistory.Repository;

import com.hyetaekon.hyetaekon.searchHistory.entity.SearchHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends CrudRepository<SearchHistory, String> {
    // 특정 사용자의 모든 검색 기록 조회
    List<SearchHistory> findByUserId(Long userId);

    // 특정 사용자의 특정 검색어 기록 삭제
    void deleteByUserIdAndId(Long userId, String id);
}
