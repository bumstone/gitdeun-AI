package com.hyetaekon.hyetaekon.publicservice.entity;

import com.hyetaekon.hyetaekon.publicservice.converter.BusinessTypeConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "business_type", indexes = {
    @Index(name = "idx_business_type_public_service_id", columnList = "public_service_id"),
    @Index(name = "idx_business_type_enum_public_service_id", columnList = "business_type_enum, public_service_id")
})
public class BusinessType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_service_id", nullable = false)
    private PublicService publicService;

    // 사업체 형태 - 회원 정보
    @Convert(converter = BusinessTypeConverter.class)
    private BusinessTypeEnum businessTypeEnum;
}