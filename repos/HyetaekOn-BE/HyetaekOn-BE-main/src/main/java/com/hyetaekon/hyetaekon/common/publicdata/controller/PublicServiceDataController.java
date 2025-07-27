package com.hyetaekon.hyetaekon.common.publicdata.controller;

import com.hyetaekon.hyetaekon.common.publicdata.dto.*;
import com.hyetaekon.hyetaekon.common.publicdata.service.PublicServiceDataServiceImpl;
import com.hyetaekon.hyetaekon.common.publicdata.util.PublicServiceDataValidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.hyetaekon.hyetaekon.common.publicdata.util.PublicDataPath.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
public class PublicServiceDataController {

  private final PublicServiceDataServiceImpl publicServiceDataService;
  private final PublicServiceDataValidate validator;

  /**
   * 공공서비스 목록 전체 동기화 (페이징 처리)
   */
  @PostMapping("/serviceList")
  public ResponseEntity<String> createAndStoreServiceList() {
    validator.validateAndHandleException(() -> {
      // 전체 서비스 목록 동기화 (페이징 처리)
      publicServiceDataService.syncPublicServiceData(SERVICE_LIST);
      return null;
    }, SERVICE_LIST);

    return ResponseEntity.status(HttpStatus.OK).body("공공서비스 목록 데이터 동기화 완료");
  }

  /**
   * 공공서비스 상세정보 전체 동기화 (페이징 처리)
   */
  @PostMapping("/serviceDetail")
  public ResponseEntity<String> createAndStoreServiceDetailList() {
    validator.validateAndHandleException(() -> {
      // 전체 상세정보 동기화 (페이징 처리)
      publicServiceDataService.syncPublicServiceDetailData(SERVICE_DETAIL);
      return null;
    }, SERVICE_DETAIL);

    return ResponseEntity.status(HttpStatus.OK).body("공공서비스 상세정보 데이터 동기화 완료");
  }

  /**
   * 공공서비스 지원조건 전체 동기화 (페이징 처리)
   */
  @PostMapping("/supportConditions")
  public ResponseEntity<String> createAndStoreSupportConditionsList() {
    validator.validateAndHandleException(() -> {
      // 전체 지원조건 동기화 (페이징 처리)
      publicServiceDataService.syncPublicServiceConditionsData(SERVICE_CONDITIONS);
      return null;
    }, SERVICE_CONDITIONS);

    return ResponseEntity.status(HttpStatus.OK).body("공공서비스 지원조건 데이터 동기화 완료");
  }

  /**
   * 페이지 단위 공공서비스 목록 조회 (테스트용)
   */
  @GetMapping("/serviceList/test")
  public ResponseEntity<List<PublicServiceDataDto.Data>> getServiceListByPage(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "100") int perPage) {

    List<PublicServiceDataDto.Data> result = validator.validateAndHandleException(() -> {
      List<PublicServiceDataDto> dtoList = publicServiceDataService.fetchPublicServiceData(SERVICE_LIST, page, perPage);

      return dtoList.stream()
          .filter(dto -> dto.getData() != null)
          .flatMap(dto -> dto.getData().stream())
          .toList();
    }, SERVICE_LIST);

    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  // 통합 동기화
  @PostMapping("/sync-all")
  public ResponseEntity<String> syncAllPublicServiceData() {
    // 순차적으로 실행
    createAndStoreServiceList();
    createAndStoreServiceDetailList();
    createAndStoreSupportConditionsList();

    // 미사용 데이터 정리
    publicServiceDataService.cleanupObsoleteServices();

    return ResponseEntity.status(HttpStatus.OK).body("모든 공공서비스 데이터 동기화 완료");
  }
}
