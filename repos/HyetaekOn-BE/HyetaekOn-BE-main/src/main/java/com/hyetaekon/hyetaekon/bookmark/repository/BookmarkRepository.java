package com.hyetaekon.hyetaekon.bookmark.repository;


import com.hyetaekon.hyetaekon.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndPublicServiceId(Long userId, String serviceId);

    Optional<Bookmark> findByUserIdAndPublicServiceId(Long userId, String serviceId);

    /*@Query("SELECT b FROM Bookmark b " +
        "JOIN FETCH b.publicService ps " +
        "JOIN FETCH b.user u " +
        "WHERE ps.id = :serviceId AND u.id = :userId")
    Optional<Bookmark> findByUserIdAndPublicServiceIdWithDetails(
        @Param("userId") Long userId,
        @Param("serviceId") Long serviceId
    );*/
}