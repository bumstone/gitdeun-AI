package com.hyetaekon.hyetaekon.user.controller;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import com.hyetaekon.hyetaekon.common.jwt.CustomUserPrincipal;
import com.hyetaekon.hyetaekon.common.jwt.JwtTokenParser;
import com.hyetaekon.hyetaekon.common.jwt.JwtTokenProvider;
import com.hyetaekon.hyetaekon.post.dto.MyPostListResponseDto;
import com.hyetaekon.hyetaekon.post.service.PostService;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.service.PublicServiceHandler;
import com.hyetaekon.hyetaekon.user.dto.*;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import com.hyetaekon.hyetaekon.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final PublicServiceHandler publicServiceHandler;
  private final PostService postService;
  private final JwtTokenParser jwtTokenParser;
  private final UserRepository userRepository;

  // 회원 가입 api
  @PostMapping("/signup")
  public ResponseEntity<UserSignUpResponseDto> registerUser(@RequestBody @Valid UserSignUpRequestDto userSignUpRequestDto) {
    UserSignUpResponseDto userSignUpResponseDto = userService.registerUser(userSignUpRequestDto);
    log.debug("회원가입 성공: realId={}", userSignUpRequestDto.getRealId());
    return ResponseEntity.status(HttpStatus.CREATED).body(userSignUpResponseDto);
  }

  // 개인 정보 조회 api
  @GetMapping("/users/me")
  public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getId();
    UserResponseDto userInfo = userService.getMyInfo(userId);
    return ResponseEntity.ok(userInfo);
  }

  // 타회원 정보 조회
  @GetMapping("/users/{userId}")
  public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long userId) {
    UserResponseDto user = userService.getUserById(userId);
    return ResponseEntity.ok(user);
  }


  // 개인 정보 수정 api
  @PutMapping("/users/me/profile")
  public ResponseEntity<UserResponseDto> updateMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody @Valid UserProfileUpdateDto profileUpdateDto
  ) {
    Long userId = userDetails.getId();
    return ResponseEntity.ok(userService.updateUserProfile(userId, profileUpdateDto));
  }

  // 비밀번호 변경 API
  @PutMapping("/users/me/password")
  public ResponseEntity<Void> updateMyPassword(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody @Valid UserPasswordUpdateDto passwordUpdateDto
  ) {
    Long userId = userDetails.getId();
    userService.updateUserPassword(userId, passwordUpdateDto);
    return ResponseEntity.ok().build();
  }

  // 회원 탈퇴
  @DeleteMapping("/users/me")
  public ResponseEntity<Void> deleteUser(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody UserDeleteRequestDto deleteRequestDto,
      @CookieValue(name = "refreshToken", required = false) String refreshToken,
      @RequestHeader("Authorization") String authHeader
  ) {
    String accessToken = authHeader.replace("Bearer ", "");

    // 인증 객체가 null인 경우 토큰에서 직접 사용자 정보 추출
    if (userDetails == null) {
      try {
        Claims claims = jwtTokenParser.parseClaims(accessToken);
        String realId = claims.getSubject();

        User user = userRepository.findByRealIdAndDeletedAtIsNull(realId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_REAL_ID));

        userService.deleteUser(user.getId(), deleteRequestDto.getDeleteReason(), accessToken, refreshToken);
      } catch (Exception e) {
        throw new GlobalException(ErrorCode.DELETE_USER_DENIED);
      }
    } else {
      // 정상적인 인증 객체가 있는 경우
      userService.deleteUser(userDetails.getId(), deleteRequestDto.getDeleteReason(), accessToken, refreshToken);
    }

    return ResponseEntity.noContent().build();
  }

  // 중복 확인 api
  @GetMapping("/users/check-duplicate")
  public boolean checkDuplicate(
      @RequestParam(value = "type") String type,
      @RequestParam(value = "value") String value) {

    return userService.checkDuplicate(type, value);
  }

  // 북마크한 서비스 목록 조회
  @GetMapping("/users/me/bookmarked/posts")
  public ResponseEntity<Page<PublicServiceListResponseDto>> getBookmarkedServices(
      @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(name = "size", defaultValue = "10") @Positive @Max(30) int size,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(publicServiceHandler.getBookmarkedServices(
        userDetails.getId(), PageRequest.of(page, size))
    );
  }

  /**
   * 내가 작성한 게시글 목록 조회
   */
  @GetMapping("/users/me/posts")
  public ResponseEntity<Page<MyPostListResponseDto>> getMyPosts(
      @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(name = "size", defaultValue = "10") @Positive @Max(30) int size,
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    Page<MyPostListResponseDto> posts = postService.getPostsByUserId(
        userDetails.getId(), PageRequest.of(page, size));

    return ResponseEntity.ok(posts);
  }

  /**
   * 내가 추천한 게시글 목록 조회
   */
  @GetMapping("/users/me/recommended/posts")
  public ResponseEntity<Page<MyPostListResponseDto>> getMyRecommendedPosts(
      @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(name = "size", defaultValue = "10") @Positive @Max(30) int size,
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    Page<MyPostListResponseDto> posts = postService.getRecommendedPostsByUserId(
        userDetails.getId(), PageRequest.of(page, size));

    return ResponseEntity.ok(posts);
  }


}
