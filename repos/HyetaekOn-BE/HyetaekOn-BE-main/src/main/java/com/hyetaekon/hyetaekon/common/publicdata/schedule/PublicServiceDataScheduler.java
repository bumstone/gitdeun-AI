package com.hyetaekon.hyetaekon.common.publicdata.schedule;

import com.hyetaekon.hyetaekon.common.publicdata.service.PublicServiceDataService;
import com.hyetaekon.hyetaekon.common.publicdata.util.PublicDataPath;
import com.hyetaekon.hyetaekon.publicservice.util.ServiceCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicServiceDataScheduler {

    private final PublicServiceDataService publicServiceDataService;
    private final ServiceCacheManager serviceCacheManager;

    /**
     * 매주 월요일 새벽 2시에 공공서비스 데이터 전체 동기화 실행
     *  1. 서비스 목록 동기화
     *  2. 서비스 상세정보 동기화
     *  3. 서비스 지원조건 동기화
     *  4. 더 이상 사용되지 않는 데이터 정리
     */
    @Scheduled(cron = "0 0 2 ? * MON")
    public void syncAllPublicServiceData() {
        log.info("공공서비스 데이터 전체 동기화 스케줄러 시작");

        try {
            // 1. 공공서비스 목록 동기화
            log.info("1. 공공서비스 목록 동기화 시작");
            publicServiceDataService.syncPublicServiceData(PublicDataPath.SERVICE_LIST);

            // 2. 공공서비스 상세정보 동기화
            log.info("2. 공공서비스 상세정보 동기화 시작");
            publicServiceDataService.syncPublicServiceDetailData(PublicDataPath.SERVICE_DETAIL);

            // 3. 공공서비스 지원조건 동기화
            log.info("3. 공공서비스 지원조건 동기화 시작");
            publicServiceDataService.syncPublicServiceConditionsData(PublicDataPath.SERVICE_CONDITIONS);

            // 4. 미사용 데이터 정리
            log.info("미사용 공공서비스 데이터 정리 시작");
            int deletedCount = publicServiceDataService.cleanupObsoleteServices();
            log.info("미사용 공공서비스 데이터 {}건 삭제 완료", deletedCount);

            // 5. 동기화 완료 후 캐시 초기화
            serviceCacheManager.clearAllServiceCaches();

            log.info("공공서비스 데이터 전체 동기화 완료");
        } catch (Exception e) {
            log.error("공공서비스 데이터 동기화 중 오류 발생", e);
        }
    }
}
