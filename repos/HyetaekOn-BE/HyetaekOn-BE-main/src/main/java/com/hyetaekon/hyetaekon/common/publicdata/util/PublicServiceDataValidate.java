package com.hyetaekon.hyetaekon.common.publicdata.util;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.common.publicdata.dto.PublicServiceConditionsDataDto;
import com.hyetaekon.hyetaekon.common.publicdata.dto.PublicServiceDataDto;
import com.hyetaekon.hyetaekon.common.publicdata.dto.PublicServiceDetailDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;


import static com.hyetaekon.hyetaekon.common.exception.ErrorCode.*;

@Slf4j
@Component
public class PublicServiceDataValidate {

    /**
     * 공공 서비스 데이터 검증 및 예외 처리
     */
    public <T> T validateAndHandleException(PublicServiceDataOperation<T> operation, PublicDataPath apiPath) {
        try {
            return operation.execute();
        } catch (InvalidFormatException e) {
            log.error("❌ 데이터 조회 실패: 파라미터 또는 서비스 키를 확인해주세요. [API_PATH: {}]", apiPath.getPath(), e);
            throw new GlobalException(INVALID_PUBLIC_SERVICE_DATA);
        } catch (HttpClientErrorException e) {
            log.error("❌ HTTP 요청 오류: [API_PATH: {}], 상태 코드: {}", apiPath.getPath(), e.getStatusCode(), e);
            throw new GlobalException(PUBLIC_SERVICE_BAD_REQUEST);
        } catch (HttpServerErrorException e) {
            log.error("❌ 서버 오류: [API_PATH: {}], 상태 코드: {}", apiPath.getPath(), e.getStatusCode(), e);
            throw new GlobalException(PUBLIC_SERVICE_SERVER_ERROR);
        } catch (RestClientException e) {
            if (e.getCause() instanceof JsonMappingException) {
                log.error("❌ JSON 매핑 오류: [API_PATH: {}]", apiPath.getPath(), e);
                throw new GlobalException(PUBLIC_SERVICE_DATA_MAPPING_ERROR);
            }
            log.error("❌ RestTemplate 오류: [API_PATH: {}]", apiPath.getPath(), e);
            throw new GlobalException(PUBLIC_SERVICE_NETWORK_ERROR);
        } catch (Exception e) {
            log.error("❌ 공공서비스 데이터 조회 중 예기치 않은 오류 발생. [API_PATH: {}]", apiPath.getPath(), e);
            throw new GlobalException(PUBLIC_SERVICE_API_ERROR);
        }
    }

    /**
     * 데이터 유효성 검증
     */
    public boolean validatePublicServiceData(PublicServiceDataDto.Data data) {
        if (data.getServiceName() == null || data.getServiceName().isEmpty() ||
            data.getServiceCategory() == null || data.getServiceCategory().isEmpty() ||
            data.getSummaryPurpose() == null || data.getSummaryPurpose().isEmpty()) {
            log.warn("⚠️ 공공 서비스 ID {}에 필수 데이터가 누락되었습니다.", data.getServiceId());
            return false;
        }
        return true;
    }

    // PublicServiceDataValidate.java 수정
    public boolean validatePublicServiceDetailData(PublicServiceDetailDataDto.Data data) {
        // 필수 필드 리스트를 먼저 확인
        boolean isValid = data.getServicePurpose() != null && !data.getServicePurpose().isEmpty() &&
            data.getSupportTarget() != null && !data.getSupportTarget().isEmpty() &&
            data.getSupportDetail() != null && !data.getSupportDetail().isEmpty() &&
            data.getSupportType() != null && !data.getSupportType().isEmpty() &&
            data.getApplicationMethod() != null && !data.getApplicationMethod().isEmpty() &&
            data.getApplicationDeadline() != null && !data.getApplicationDeadline().isEmpty() &&
            data.getGoverningAgency() != null && !data.getGoverningAgency().isEmpty() &&
            data.getContactInfo() != null && !data.getContactInfo().isEmpty();

        if (!isValid) {
            log.warn("⚠️ 공공 서비스 상세내용 ID {}에 필수 데이터가 누락되었습니다.", data.getServiceId());
            return false;
        }
        return true;
    }

    public boolean validatePublicServiceConditionsData(PublicServiceConditionsDataDto.Data data) {
        // 성별 조건 확인
        boolean hasGenderCondition = "Y".equals(data.getTargetGenderMale()) || "Y".equals(data.getTargetGenderFemale());

        // 성별 조건이 없는 경우, 기본값으로 모두 Y 설정
        if (!hasGenderCondition) {
            log.info("ℹ️ 공공 서비스 지원조건 ID {}에 성별 지원 조건이 없습니다. 기본값으로 남성/여성 모두 Y로 설정합니다.", data.getServiceId());
            data.setTargetGenderMale("Y");
            data.setTargetGenderFemale("Y");
            // hasGenderCondition = true;
        }

        // 다른 조건들 검사 (경고만 로깅하고 실제로는 모든 데이터 허용)
        boolean hasSpecialGroupCondition =
            "Y".equals(data.getJA0401()) || "Y".equals(data.getJA0402()) ||
                "Y".equals(data.getJA0403()) || "Y".equals(data.getJA0404()) ||
                "Y".equals(data.getJA0328()) || "Y".equals(data.getJA0329()) ||
                "Y".equals(data.getJA0330());

        boolean hasFamilyTypeCondition =
            "Y".equals(data.getJA0411()) || "Y".equals(data.getJA0412()) ||
                "Y".equals(data.getJA0413()) || "Y".equals(data.getJA0414());

        boolean hasOccupationCondition =
            "Y".equals(data.getJA0313()) || "Y".equals(data.getJA0314()) ||
                "Y".equals(data.getJA0315()) || "Y".equals(data.getJA0316()) ||
                "Y".equals(data.getJA0317()) || "Y".equals(data.getJA0318()) ||
                "Y".equals(data.getJA0319()) || "Y".equals(data.getJA0320()) ||
                "Y".equals(data.getJA0326()) || "Y".equals(data.getJA0327());

        boolean hasBusinessTypeCondition =
            "Y".equals(data.getJA1101()) || "Y".equals(data.getJA1102()) ||
                "Y".equals(data.getJA1103()) || "Y".equals(data.getJA1201()) ||
                "Y".equals(data.getJA1202()) || "Y".equals(data.getJA1299()) ||
                "Y".equals(data.getJA2101()) || "Y".equals(data.getJA2102()) ||
                "Y".equals(data.getJA2103()) || "Y".equals(data.getJA2201()) ||
                "Y".equals(data.getJA2202()) || "Y".equals(data.getJA2203()) ||
                "Y".equals(data.getJA2299());

        if (!(hasSpecialGroupCondition || hasFamilyTypeCondition ||
            hasOccupationCondition || hasBusinessTypeCondition)) {
            log.warn("⚠️ 공공 서비스 지원조건 ID {}에 성별 외 다른 지원 조건이 없습니다.", data.getServiceId());
            // 경고만 로깅하고 데이터는 허용
        }

        // 항상 true 반환하여 모든 데이터 허용
        return true;
    }

    /**
     * 데이터 조회 및 처리 작업을 위한 함수형 인터페이스
     */
    @FunctionalInterface
    public interface PublicServiceDataOperation<T> {
        T execute() throws Exception;
    }
}
