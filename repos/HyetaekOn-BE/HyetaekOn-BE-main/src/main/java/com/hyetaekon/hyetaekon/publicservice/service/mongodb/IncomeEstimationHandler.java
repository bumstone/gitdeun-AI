package com.hyetaekon.hyetaekon.publicservice.service.mongodb;

import com.hyetaekon.hyetaekon.publicservice.entity.BusinessTypeEnum;
import com.hyetaekon.hyetaekon.publicservice.entity.OccupationEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncomeEstimationHandler {

    public String determineIncomeLevelFromJob(String job) {
        if (job == null || job.isEmpty()) {
            return "MIDDLE"; // 기본값
        }

        // OccupationEnum에서 일치하는 항목 찾기
        for (OccupationEnum occupation : OccupationEnum.values()) {
            if (occupation.getType().equals(job)) {
                return getIncomeByOccupation(occupation);
            }
        }

        // BusinessTypeEnum에서 일치하는 항목 찾기
        for (BusinessTypeEnum businessType : BusinessTypeEnum.values()) {
            if (businessType.getType().equals(job)) {
                return getIncomeByBusinessType(businessType);
            }
        }

        // 일치하는 것이 없으면 일반 직업 분류로 추정
        return getIncomeByGenericJob(job);
    }

    private String getIncomeByOccupation(OccupationEnum occupation) {
        switch (occupation) {
            case IS_ELEMENTARY_STUDENT:
            case IS_MIDDLE_SCHOOL_STUDENT:
            case IS_HIGH_SCHOOL_STUDENT:
            case IS_JOB_SEEKER:
                return "LOW";

            case IS_UNIVERSITY_STUDENT:
                return "MIDDLE_LOW";

            case IS_FARMER:
            case IS_FISHERMAN:
            case IS_STOCK_BREEDER:
            case IS_FORESTER:
                return "MIDDLE";

            case IS_WORKER:
                return "MIDDLE_HIGH";

            default:
                return "MIDDLE";
        }
    }

    private String getIncomeByBusinessType(BusinessTypeEnum businessType) {
        switch (businessType) {
            case IS_BUSINESS_HARDSHIP:
                return "LOW";

            case IS_STARTUP_PREPARATION:
            case IS_FOOD_INDUSTRY:
                return "MIDDLE_LOW";

            case IS_BUSINESS_OPERATING:
            case IS_OTHER_INDUSTRY:
            case IS_OTHER_INDUSTRY_TYPE:
                return "MIDDLE";

            case IS_MANUFACTURING_INDUSTRY:
            case IS_MANUFACTURING_INDUSTRY_TYPE:
            case IS_SMALL_MEDIUM_ENTERPRISE:
                return "MIDDLE_HIGH";

            case IS_INFORMATION_TECHNOLOGY_INDUSTRY:
            case IS_ORGANIZATION:
            case IS_SOCIAL_WELFARE_INSTITUTION:
                return "HIGH";

            default:
                return "MIDDLE";
        }
    }

    private String getIncomeByGenericJob(String job) {
        // 일반적인 직업 키워드 기반 추정
        String lowercaseJob = job.toLowerCase();

        if (lowercaseJob.contains("학생") || lowercaseJob.contains("구직자")
            || lowercaseJob.contains("실업자")) {
            return "LOW";
        }

        if (lowercaseJob.contains("공무원") || lowercaseJob.contains("인턴")) {
            return "MIDDLE_LOW";
        }

        if (lowercaseJob.contains("직장인") || lowercaseJob.contains("회사원")
            || lowercaseJob.contains("프리랜서")) {
            return "MIDDLE";
        }

        if (lowercaseJob.contains("전문직") || lowercaseJob.contains("관리직")) {
            return "MIDDLE_HIGH";
        }

        if (lowercaseJob.contains("임원") || lowercaseJob.contains("의사")
            || lowercaseJob.contains("변호사")) {
            return "HIGH";
        }

        return "MIDDLE_LOW";  // 기본값
    }
}
