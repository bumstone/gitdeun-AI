package com.hyetaekon.hyetaekon.publicservice.entity;

import com.hyetaekon.hyetaekon.publicservice.converter.OccupationConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "occupation", indexes = {
    @Index(name = "idx_occupation_public_service_id", columnList = "public_service_id"),
    @Index(name = "idx_occupation_enum_public_service_id", columnList = "occupation_enum, public_service_id")
})
public class Occupation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_service_id", nullable = false)
    private PublicService publicService;

    // 대상 직종 - 회원 정보
    @Convert(converter = OccupationConverter.class)
    private OccupationEnum occupationEnum;
}