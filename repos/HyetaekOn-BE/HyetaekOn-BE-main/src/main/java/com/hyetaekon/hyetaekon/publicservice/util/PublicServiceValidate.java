package com.hyetaekon.hyetaekon.publicservice.util;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import com.hyetaekon.hyetaekon.publicservice.entity.ServiceCategory;
import com.hyetaekon.hyetaekon.publicservice.repository.PublicServiceRepository;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PublicServiceValidate {
    public final PublicServiceRepository publicServiceRepository;
    public final UserRepository userRepository;

    public PublicService validateServiceById(String serviceId) {
        return publicServiceRepository.findById(serviceId)
            .orElseThrow(() -> new GlobalException(ErrorCode.SERVICE_NOT_FOUND_BY_ID));
    }

    public User validateUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));
    }

    public ServiceCategory validateServiceCategory(String categoryName) {
        /*try {
            return ServiceCategory.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            throw new GlobalException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND);
        }*/
        for (ServiceCategory category : ServiceCategory.values()) {
            if (category.getType().equals(categoryName)) {
                return category;
            }
        }
        throw new GlobalException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND);
    }

    /**
     * 서비스 상세 정보의 완전성 검증
     * @param service 검증할 공공서비스 객체
     * @return 상세 정보가 불완전하면 true, 충분히 완전하면 false 반환
     */
    public boolean isDetailInformationIncomplete(PublicService service) {
        // 필수 필드 중 일정 개수 이상 누락된 경우 true 반환
        int nullCount = 0;

        if (service.getServicePurpose() == null || service.getServicePurpose().isEmpty()) nullCount++;
        if (service.getSupportTarget() == null || service.getSupportTarget().isEmpty()) nullCount++;
        if (service.getSupportDetail() == null || service.getSupportDetail().isEmpty()) nullCount++;
        if (service.getSupportType() == null || service.getSupportType().isEmpty()) nullCount++;
        if (service.getApplicationMethod() == null || service.getApplicationMethod().isEmpty()) nullCount++;
        if (service.getApplicationDeadline() == null || service.getApplicationDeadline().isEmpty()) nullCount++;
        if (service.getGoverningAgency() == null || service.getGoverningAgency().isEmpty()) nullCount++;
        if (service.getContactInfo() == null || service.getContactInfo().isEmpty()) nullCount++;

        // 필수 필드 중 3개 이상 누락되면 불완전한 데이터로 판단
        return nullCount >= 3;
    }
}
