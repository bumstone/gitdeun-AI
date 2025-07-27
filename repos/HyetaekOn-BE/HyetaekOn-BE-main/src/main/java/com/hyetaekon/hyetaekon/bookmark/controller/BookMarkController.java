package com.hyetaekon.hyetaekon.bookmark.controller;

import com.hyetaekon.hyetaekon.bookmark.service.BookmarkService;
import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/services/{serviceId}/bookmark")
@RequiredArgsConstructor
public class BookMarkController {
  private final BookmarkService bookmarkService;

  // 북마크 추가
  @PostMapping
  public ResponseEntity<Void> addBookmark(
      @PathVariable("serviceId") String serviceId,
      @AuthenticationPrincipal CustomUserDetails customUserDetails
  ) {
    bookmarkService.addBookmark(serviceId, customUserDetails.getId());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 북마크 제거
  @DeleteMapping
  public ResponseEntity<Void> removeBookmark(
      @PathVariable("serviceId") String serviceId,
      @AuthenticationPrincipal CustomUserDetails customUserDetails
  ) {
    bookmarkService.removeBookmark(serviceId, customUserDetails.getId());
    return ResponseEntity.noContent().build();
  }

}
