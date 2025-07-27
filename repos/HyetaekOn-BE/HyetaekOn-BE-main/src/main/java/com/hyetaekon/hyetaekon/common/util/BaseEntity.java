package com.hyetaekon.hyetaekon.common.util;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@MappedSuperclass
public class BaseEntity {

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(0)")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "modified_at", nullable = false, columnDefinition = "DATETIME(0)")
  private LocalDateTime modifiedAt;

}