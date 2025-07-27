package com.hyetaekon.hyetaekon.common.publicdata.service;

import com.hyetaekon.hyetaekon.common.publicdata.dto.*;
import com.hyetaekon.hyetaekon.common.publicdata.util.PublicDataPath;

import java.util.List;

public interface PublicServiceDataService {

    // 페이징 파라미터를 포함한 API 호출 메서드
    List<PublicServiceDataDto> fetchPublicServiceData(PublicDataPath apiPath, int page, int perPage);
    List<PublicServiceDetailDataDto> fetchPublicServiceDetailData(PublicDataPath apiPath, int page, int perPage);
    List<PublicServiceConditionsDataDto> fetchPublicServiceConditionsData(PublicDataPath apiPath, int page, int perPage);

    // 전체 데이터 동기화 메서드 (스케줄러용)
    void syncPublicServiceData(PublicDataPath apiPath);
    void syncPublicServiceDetailData(PublicDataPath apiPath);
    void syncPublicServiceConditionsData(PublicDataPath apiPath);

    // 데이터 저장 메서드
    List<PublicServiceDataDto.Data> upsertServiceData(List<PublicServiceDataDto.Data> dataList);
    List<PublicServiceDetailDataDto.Data> upsertServiceDetailData(List<PublicServiceDetailDataDto.Data> dataList);
    List<PublicServiceConditionsDataDto.Data> upsertSupportConditionsData(List<PublicServiceConditionsDataDto.Data> dataList);

    // 미사용 데이터 정리 메서드
    int cleanupObsoleteServices();
}