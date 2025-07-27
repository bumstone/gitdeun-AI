package com.hyetaekon.hyetaekon.bookmark.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hyetaekon.hyetaekon.common.util.BaseEntity;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import com.hyetaekon.hyetaekon.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bookmark", indexes = {
    @Index(name = "idx_bookmark_user_public_service", columnList = "user_id, public_service_id", unique = true), // 주요 조회 조건 및 중복 방지
    @Index(name = "idx_bookmark_public_service_id", columnList = "public_service_id") // 서비스기준 북마크 목록 조회
})
public class Bookmark extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonIgnore
  @JoinColumn(name = "public_service_id", nullable = false)
  private PublicService publicService;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

}