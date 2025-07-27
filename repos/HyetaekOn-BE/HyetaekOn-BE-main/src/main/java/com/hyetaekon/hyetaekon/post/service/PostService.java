package com.hyetaekon.hyetaekon.post.service;

import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.common.s3bucket.service.S3BucketService;
import com.hyetaekon.hyetaekon.post.dto.*;
import com.hyetaekon.hyetaekon.post.entity.Post;
import com.hyetaekon.hyetaekon.post.entity.PostImage;
import com.hyetaekon.hyetaekon.post.entity.PostType;
import com.hyetaekon.hyetaekon.post.mapper.PostImageMapper;
import com.hyetaekon.hyetaekon.post.mapper.PostMapper;
import com.hyetaekon.hyetaekon.post.repository.PostImageRepository;
import com.hyetaekon.hyetaekon.post.repository.PostRepository;
import com.hyetaekon.hyetaekon.recommend.repository.RecommendRepository;
import com.hyetaekon.hyetaekon.user.entity.PointActionType;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import com.hyetaekon.hyetaekon.user.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.hyetaekon.hyetaekon.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final RecommendRepository recommendRepository;
    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final S3BucketService s3BucketService;
    private final UserPointService userPointService;

    // 이미지 업로드 제한 설정
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILES_COUNT = 5; // 최대 5개 이미지
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/gif"
    );

    /**
     * 전체 게시글 목록 조회 (제목 검색 + 정렬)
     */
    public Page<PostListResponseDto> getAllPosts(String keyword, String sortBy, String direction, Pageable pageable) {
        Pageable sortedPageable = createSortedPageable(pageable, sortBy, direction);

        if (keyword != null && !keyword.trim().isEmpty()) {
            return postRepository.findByTitleContainingAndDeletedAtIsNull(keyword, sortedPageable)
                    .map(postMapper::toPostListDto);
        } else {
            return postRepository.findByDeletedAtIsNull(sortedPageable)
                    .map(postMapper::toPostListDto);
        }
    }

    /**
     * 특정 타입 게시글 목록 조회 (제목 검색 + 정렬)
     */
    public Page<PostListResponseDto> getPostsByType(PostType postType, String keyword, String sortBy, String direction, Pageable pageable) {
        Pageable sortedPageable = createSortedPageable(pageable, sortBy, direction);

        if (keyword != null && !keyword.trim().isEmpty()) {
            return postRepository.findByPostTypeAndTitleContainingAndDeletedAtIsNull(postType, keyword, sortedPageable)
                    .map(postMapper::toPostListDto);
        } else {
            return postRepository.findByPostTypeAndDeletedAtIsNull(postType, sortedPageable)
                    .map(postMapper::toPostListDto);
        }
    }

    /**
     * 정렬 기준을 적용한 Pageable 생성
     */
    private Pageable createSortedPageable(Pageable pageable, String sortBy, String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /**
     * 특정 게시글 상세 조회(로그인 시)
     */
    @Transactional
    public PostDetailResponseDto getPostById(Long postId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId));

        // 조회수 증가
        post.incrementViewCnt();

        // 사용자의 추천 여부 확인
        boolean recommended = recommendRepository.existsByUserIdAndPostId(userId, postId);

        // DTO 변환 및 추천 여부 설정
        PostDetailResponseDto responseDto = postMapper.toPostDetailDto(post);
        responseDto.setRecommended(recommended);

        return responseDto;
    }

    /**
     * 게시글 생성(로그인 시)
     */
    @Transactional
    public PostDetailResponseDto createPost(PostCreateRequestDto requestDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        PostType postType = PostType.fromString(requestDto.getPostType());
        log.info("Received postType: '{}'", requestDto.getPostType());

        // 포인트 부여 로직을 게시글 저장 전에 수행
        boolean isFirstGreetingPost = false;
        try {
            if (postType == PostType.GREETING) {
                // 첫 인사 게시글 여부 확인 (게시글 저장 전에 체크)
                isFirstGreetingPost = !postRepository.existsByUser_IdAndPostTypeAndDeletedAtIsNull(userId, PostType.GREETING);
                log.info("인사 게시글 작성 체크 - 사용자 ID: {}, 첫 인사 게시글 여부: {}", userId, isFirstGreetingPost);
            }
        } catch (Exception e) {
            log.error("포인트 부여 체크 중 오류 발생 - 사용자 ID: {}", userId, e);
        }

        // 게시글 저장
        Post post = postMapper.toEntity(requestDto);
        post.setUser(user);
        post.setPostType(postType);

        Post savedPost = postRepository.save(post);

        // 이미지 처리
        if (requestDto.getImages() != null && !requestDto.getImages().isEmpty()) {
            List<PostImage> postImages = processPostImages(requestDto.getImages(), savedPost);
            if (!postImages.isEmpty()) {
                postImageRepository.saveAll(postImages);
                savedPost.setPostImages(postImages);
            }
        }

        // 게시글 저장 후 포인트 부여
        try {
            if (postType == PostType.GREETING) {
                if (isFirstGreetingPost) {
                    // 첫 인사 게시글인 경우 100점
                    userPointService.addPointForAction(userId, PointActionType.FIRST_GREETING_POST_CREATION);
                    log.info("첫 인사 게시글 작성 완료 - 사용자 ID: {}, 부여 포인트: 100점", userId);
                } else {
                    // 추가 인사 게시글인 경우 20점
                    userPointService.addPointForAction(userId, PointActionType.POST_CREATION);
                    log.info("추가 인사 게시글 작성 완료 - 사용자 ID: {}, 부여 포인트: 20점", userId);
                }
            } else {
                // 일반 게시글인 경우 20점
                userPointService.addPointForAction(userId, PointActionType.POST_CREATION);
                log.info("일반 게시글 작성 완료 - 사용자 ID: {}, 부여 포인트: 20점", userId);
            }
        } catch (Exception e) {
            log.error("포인트 부여 중 오류 발생 - 사용자 ID: {}, 게시글 ID: {}", userId, savedPost.getId(), e);
            // 포인트 부여 실패해도 게시글 생성은 성공으로 처리
        }

        return postMapper.toPostDetailDto(savedPost);
    }


    /**
     * 게시글 수정 (본인만 가능)
     */
    @Transactional
    public PostDetailResponseDto updatePost(Long postId, PostUpdateRequestDto updateDto, Long userId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId));

        // 작성자 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("게시글 수정 권한이 없습니다");
        }

        // 영문 타입 코드로 PostType 찾기
        if (updateDto.getPostType() != null) {
            PostType postType = PostType.fromString(updateDto.getPostType());
            post.setPostType(postType);
        }

        // 기본 정보 업데이트(title, content, urlTitle, urlPath, tags)
        postMapper.updatePostFromDto(updateDto, post);

        // 이미지 업데이트 처리
        if (hasImageChanges(updateDto)) {  // null-safe
            updatePostImages(post, updateDto.getKeepImageIds(), updateDto.getNewImages());
        }

        // 업데이트 후 최신 상태로 다시 조회
        Post updatedPost = postRepository.findByIdAndDeletedAtIsNull(postId)
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다"));

        return postMapper.toPostDetailDto(updatedPost);
    }

    // 이미지 변경사항 감지 메서드 (helper)
    private boolean hasImageChanges(PostUpdateRequestDto updateDto) {
        // 새로운 이미지가 있거나, 유지할 이미지 목록이 명시되어 있으면 변경으로 간주
        boolean hasNewImages = updateDto.getNewImages() != null && !updateDto.getNewImages().isEmpty();
        boolean hasKeepImageIds = updateDto.getKeepImageIds() != null && !updateDto.getKeepImageIds().isEmpty();

        return hasNewImages || hasKeepImageIds;
    }

    private void updatePostImages(Post post, List<Long> keepImageIds, List<MultipartFile> newImages) {
        // 기존 이미지 중 유지하지 않는 것만 삭제
        List<PostImage> existingImages = postImageRepository.findByPostAndDeletedAtIsNull(post);

        if (keepImageIds == null) {
            keepImageIds = new ArrayList<>();
        }

        // 유지하지 않는 이미지만 soft delete
        for (PostImage existingImage : existingImages) {
            if (!keepImageIds.contains(existingImage.getId())) {
                existingImage.softDelete();
            }
        }
        postImageRepository.saveAll(existingImages);

        // 새 이미지 추가
        if (newImages != null && !newImages.isEmpty()) {
            List<PostImage> newPostImages = processPostImages(newImages, post);
            postImageRepository.saveAll(newPostImages);
        }
    }

    /**
     * 게시글 삭제 (본인 또는 관리자만 가능)
     */
    @Transactional
    public void deletePost(Long postId, Long userId, String role) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId));

        // 작성자 또는 관리자 확인
        boolean isOwner = post.getUser().getId().equals(userId);
        boolean isAdmin = "ROLE_ADMIN".equals(role);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("게시글 삭제 권한이 없습니다");
        }

        // Soft Delete 처리
        post.delete();

        // 모든 이미지 soft delete 처리
        List<PostImage> images = postImageRepository.findByPostAndDeletedAtIsNull(post);
        for (PostImage image : images) {
            image.softDelete();
        }

        postRepository.save(post);
        postImageRepository.saveAll(images);
    }

    /**
     * 이미지 처리를 위한 private 메서드
     */
    private List<PostImage> processPostImages(List<MultipartFile> images, Post post) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }

        // 이미지 유효성 검증
        validateImages(images);

        // 이미지 업로드 및 엔티티 변환
        try {
            List<String> uploadedUrls = s3BucketService.upload(images, "posts/" + post.getId());
            return postImageMapper.toEntityList(uploadedUrls, post);
        } catch (Exception e) {
            log.error("이미지 업로드 실패: ", e);
            throw new GlobalException(FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 이미지 유효성 검증
     */
    private void validateImages(List<MultipartFile> images) {
        // 이미지 파일 개수 제한
        if (images.size() > MAX_FILES_COUNT) {
            throw new GlobalException(FILE_COUNT_EXCEEDED);
        }

        for (MultipartFile image : images) {
            // 파일 크기 검증
            if (image.getSize() > MAX_FILE_SIZE) {
                throw new GlobalException(FILE_SIZE_EXCEEDED);
            }

            // 파일 타입 검증
            String contentType = image.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new GlobalException(INVALID_FILE_TYPE);
            }
        }
    }

    /**
     * 사용자가 작성한 게시글 목록 조회
     */
    public Page<MyPostListResponseDto> getPostsByUserId(Long userId, Pageable pageable) {
        return postRepository.findMyPostsOptimized(userId, pageable)
            .map(postMapper::toMyPostListDto);
    }

    /**
     * 사용자가 추천한 게시글 목록 조회
     */
    public Page<MyPostListResponseDto> getRecommendedPostsByUserId(Long userId, Pageable pageable) {
        return postRepository.findRecommendedPostsOptimized(userId, pageable)
            .map(post -> {
                MyPostListResponseDto dto = postMapper.toMyPostListDto(post);
                return dto;
            });
    }


}
