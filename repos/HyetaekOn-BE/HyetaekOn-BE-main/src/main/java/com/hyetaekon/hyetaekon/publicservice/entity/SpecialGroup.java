package com.hyetaekon.hyetaekon.publicservice.entity;

import com.hyetaekon.hyetaekon.publicservice.converter.SpecialGroupConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "special_group", indexes = {
    @Index(name = "idx_special_group_public_service_id", columnList = "public_service_id"),
    @Index(name = "idx_special_group_enum_public_service_id", columnList = "special_group_enum, public_service_id")
})
public class SpecialGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_service_id", nullable = false)
    private PublicService publicService;

    // 특수 대상 그룹 - 검색 + 해시태그
    @Convert(converter = SpecialGroupConverter.class)  // 명시적 선언
    private SpecialGroupEnum specialGroupEnum;
}
