package com.hyetaekon.hyetaekon.publicservice.repository;


import com.hyetaekon.hyetaekon.publicservice.entity.FamilyTypeEnum;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import com.hyetaekon.hyetaekon.publicservice.entity.ServiceCategory;
import com.hyetaekon.hyetaekon.publicservice.entity.SpecialGroupEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicServiceRepository extends JpaRepository<PublicService, String> {

    List<PublicService> findTop6ByOrderByBookmarkCntDesc();

    Optional<PublicService> findById(String serviceId);

    int deleteByIdNotIn(List<String> Ids);

    @Query("SELECT DISTINCT ps FROM PublicService ps " +
        "LEFT JOIN ps.specialGroups sg " +
        "LEFT JOIN ps.familyTypes ft " +
        "WHERE (:#{#categories == null || #categories.isEmpty()} = true OR ps.serviceCategory IN :categories) " +
        "AND (:#{#specialGroupEnums == null || #specialGroupEnums.isEmpty()} = true OR (sg IS NOT NULL AND sg.specialGroupEnum IN :specialGroupEnums)) " +
        "AND (:#{#familyTypeEnums == null || #familyTypeEnums.isEmpty()} = true OR (ft IS NOT NULL AND ft.familyTypeEnum IN :familyTypeEnums))")
    Page<PublicService> findWithFilters(
        @Param("categories") List<ServiceCategory> categories,
        @Param("specialGroupEnums") List<SpecialGroupEnum> specialGroupEnums,
        @Param("familyTypeEnums") List<FamilyTypeEnum> familyTypeEnums,
        Pageable pageable
    );

    // 사용자의 북마크 공공서비스 목록 페이지
    @Query("SELECT p FROM PublicService p JOIN p.bookmarks b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    Page<PublicService> findByBookmarks_User_Id(@Param("userId") Long userId, Pageable pageable);
}
