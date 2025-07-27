package com.hyetaekon.hyetaekon.common.publicdata.service;

import com.hyetaekon.hyetaekon.common.publicdata.dto.PublicServiceConditionsDataDto;
import com.hyetaekon.hyetaekon.common.publicdata.util.PublicDataPath;
import com.hyetaekon.hyetaekon.publicservice.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.hyetaekon.hyetaekon.common.publicdata.util.PublicDataConstants.SUBSIDY_DATA_END_POINT;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicServiceDataProviderService {
    @Value("${public-data.public-service}")
    private String serviceKey;

    /**
     * 특수 그룹 정보 업데이트
     */
    void updateSpecialGroups(PublicService publicService, PublicServiceConditionsDataDto.Data data) {
        // 기존 특수 그룹 정보 삭제
        publicService.getSpecialGroups().clear();

        // 새로운 특수 그룹 정보 추가
        List<SpecialGroup> specialGroups = new ArrayList<>();

        if ("Y".equals(data.getJA0401())) {
            log.debug("다문화가족 특수 그룹 추가");
            specialGroups.add(createSpecialGroup(publicService, SpecialGroupEnum.IS_MULTI_CULTURAL));
        }
        if ("Y".equals(data.getJA0402())) {
            log.debug("북한이탈주민 특수 그룹 추가");
            specialGroups.add(createSpecialGroup(publicService, SpecialGroupEnum.IS_NORTH_KOREAN_DEFECTOR));
        }
        if ("Y".equals(data.getJA0403())) {
            log.debug("한부모가정/조손가정 특수 그룹 추가");
            specialGroups.add(createSpecialGroup(publicService, SpecialGroupEnum.IS_SINGLE_PARENT_FAMILY));
        }
        if ("Y".equals(data.getJA0404())) {
            log.debug("1인가구 특수 그룹 추가");
            specialGroups.add(createSpecialGroup(publicService, SpecialGroupEnum.IS_SINGLE_MEMBER_HOUSEHOLD));
        }
        if ("Y".equals(data.getJA0328())) {
            log.debug("장애인 특수 그룹 추가");
            specialGroups.add(createSpecialGroup(publicService, SpecialGroupEnum.IS_DISABLED));
        }
        if ("Y".equals(data.getJA0329())) {
            log.debug("국가보훈대상자 특수 그룹 추가");
            specialGroups.add(createSpecialGroup(publicService, SpecialGroupEnum.IS_NATIONAL_MERIT_RECIPIENT));
        }
        if ("Y".equals(data.getJA0330())) {
            log.debug("질병/질환자 특수 그룹 추가");
            specialGroups.add(createSpecialGroup(publicService, SpecialGroupEnum.IS_CHRONIC_ILLNESS));
        }

        publicService.getSpecialGroups().addAll(specialGroups);
    }

    /**
     * 특수 그룹 객체 생성
     */
    private SpecialGroup createSpecialGroup(PublicService publicService, SpecialGroupEnum specialGroupEnum) {
        return SpecialGroup.builder()
            .publicService(publicService)
            .specialGroupEnum(specialGroupEnum)
            .build();
    }

    /**
     * 가족 유형 정보 업데이트
     */
    void updateFamilyTypes(PublicService publicService, PublicServiceConditionsDataDto.Data data) {
        // 기존 가족 유형 정보 삭제
        publicService.getFamilyTypes().clear();

        // 새로운 가족 유형 정보 추가
        List<FamilyType> familyTypes = new ArrayList<>();

        /*if ("Y".equals(data.getJA0410())) {
            familyTypes.add(createFamilyType(publicService, FamilyTypeEnum.IS_NOT_APPLICABLE));
        }*/
        if ("Y".equals(data.getJA0411())) {
            familyTypes.add(createFamilyType(publicService, FamilyTypeEnum.IS_MULTI_CHILDREN_FAMILY));
        }
        if ("Y".equals(data.getJA0412())) {
            familyTypes.add(createFamilyType(publicService, FamilyTypeEnum.IS_NON_HOUSING_HOUSEHOLD));
        }
        if ("Y".equals(data.getJA0413())) {
            familyTypes.add(createFamilyType(publicService, FamilyTypeEnum.IS_NEW_RESIDENCE));
        }
        if ("Y".equals(data.getJA0414())) {
            familyTypes.add(createFamilyType(publicService, FamilyTypeEnum.IS_EXTENDED_FAMILY));
        }

        publicService.getFamilyTypes().addAll(familyTypes);
    }

    /**
     * 가족 유형 객체 생성
     */
    private FamilyType createFamilyType(PublicService publicService, FamilyTypeEnum familyTypeEnum) {
        return FamilyType.builder()
            .publicService(publicService)
            .familyTypeEnum(familyTypeEnum)
            .build();
    }

    /**
     * 직업 유형 정보 업데이트
     */
    void updateOccupations(PublicService publicService, PublicServiceConditionsDataDto.Data data) {
        // 기존 직업 유형 정보 삭제
        publicService.getOccupations().clear();

        // 새로운 직업 유형 정보 추가
        List<Occupation> occupations = new ArrayList<>();

        if ("Y".equals(data.getJA0313())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_FARMER));
        }
        if ("Y".equals(data.getJA0314())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_FISHERMAN));
        }
        if ("Y".equals(data.getJA0315())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_STOCK_BREEDER));
        }
        if ("Y".equals(data.getJA0316())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_FORESTER));
        }
        if ("Y".equals(data.getJA0317())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_ELEMENTARY_STUDENT));
        }
        if ("Y".equals(data.getJA0318())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_MIDDLE_SCHOOL_STUDENT));
        }
        if ("Y".equals(data.getJA0319())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_HIGH_SCHOOL_STUDENT));
        }
        if ("Y".equals(data.getJA0320())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_UNIVERSITY_STUDENT));
        }
        if ("Y".equals(data.getJA0326())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_WORKER));
        }
        if ("Y".equals(data.getJA0327())) {
            occupations.add(createOccupation(publicService, OccupationEnum.IS_JOB_SEEKER));
        }

        publicService.getOccupations().addAll(occupations);
    }

    /**
     * 직업 유형 객체 생성
     */
    private Occupation createOccupation(PublicService publicService, OccupationEnum occupationEnum) {
        return Occupation.builder()
            .publicService(publicService)
            .occupationEnum(occupationEnum)
            .build();
    }

    /**
     * 사업체 유형 정보 업데이트
     */
    void updateBusinessTypes(PublicService publicService, PublicServiceConditionsDataDto.Data data) {
        // 기존 사업체 유형 정보 삭제
        publicService.getBusinessTypes().clear();

        // 새로운 사업체 유형 정보 추가
        List<BusinessType> businessTypes = new ArrayList<>();

        if ("Y".equals(data.getJA1101())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_STARTUP_PREPARATION));
        }
        if ("Y".equals(data.getJA1102())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_BUSINESS_OPERATING));
        }
        if ("Y".equals(data.getJA1103())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_BUSINESS_HARDSHIP));
        }
        if ("Y".equals(data.getJA1201())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_FOOD_INDUSTRY));
        }
        if ("Y".equals(data.getJA1202())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_MANUFACTURING_INDUSTRY));
        }
        if ("Y".equals(data.getJA1299())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_OTHER_INDUSTRY));
        }
        if ("Y".equals(data.getJA2101())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_SMALL_MEDIUM_ENTERPRISE));
        }
        if ("Y".equals(data.getJA2102())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_SOCIAL_WELFARE_INSTITUTION));
        }
        if ("Y".equals(data.getJA2103())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_ORGANIZATION));
        }
        if ("Y".equals(data.getJA2201())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_MANUFACTURING_INDUSTRY_TYPE));
        }
        if ("Y".equals(data.getJA2202())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_AGRICULTURAL_INDUSTRY));
        }
        if ("Y".equals(data.getJA2203())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_INFORMATION_TECHNOLOGY_INDUSTRY));
        }
        if ("Y".equals(data.getJA2299())) {
            businessTypes.add(createBusinessType(publicService, BusinessTypeEnum.IS_OTHER_INDUSTRY_TYPE));
        }

        publicService.getBusinessTypes().addAll(businessTypes);
    }

    /**
     * 사업체 유형 객체 생성
     */
    private BusinessType createBusinessType(PublicService publicService, BusinessTypeEnum businessTypeEnum) {
        return BusinessType.builder()
            .publicService(publicService)
            .businessTypeEnum(businessTypeEnum)
            .build();
    }

    /**
     * 공공데이터 URI 생성 메서드
     */
    public URI createUri(PublicDataPath publicDataPath, String... params) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
                SUBSIDY_DATA_END_POINT + publicDataPath.getPath())
            .queryParam("serviceKey", serviceKey);

        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                uriBuilder.queryParam(params[i], params[i + 1]);
            }
        }

        URI uri = uriBuilder.build(true).toUri();
        log.debug("생성된 URI: {}", uri);

        return uri; // true 파라미터는 인코딩을 활성화
    }
}
