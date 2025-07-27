package com.hyetaekon.hyetaekon.post.mapper;

import com.hyetaekon.hyetaekon.post.dto.*;
import com.hyetaekon.hyetaekon.post.entity.Post;
import com.hyetaekon.hyetaekon.post.entity.PostImage;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = PostImageMapper.class)
public interface PostMapper {

    // ✅ 게시글 목록용 DTO 변환
    @Mapping(source = "id", target = "postId")
    @Mapping(source = "user.nickname", target = "nickName")
    @Mapping(target = "title", expression = "java(post.getDisplayTitle())")
    @Mapping(source = "postType.koreanName", target = "postType")
    @Mapping(source = "recommendCnt", target = "recommendCnt")
    @Mapping(source = "user.id", target = "userId") // 🔥 추가
    PostListResponseDto toPostListDto(Post post);

    // ✅ 마이페이지용 게시글 DTO
    @Mapping(source = "id", target = "postId")
    @Mapping(source = "user.nickname", target = "nickName")
    @Mapping(target = "title", expression = "java(post.getDisplayTitle())")
    @Mapping(target = "content", expression = "java(post.getDisplayContent())")
    MyPostListResponseDto toMyPostListDto(Post post);

    // ✅ 게시글 생성 시 DTO → Entity 변환
    Post toEntity(PostCreateRequestDto createDto);

    // ✅ 게시글 수정 시 일부 값만 업데이트 (null 무시)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePostFromDto(PostUpdateRequestDto updateDto, @MappingTarget Post post);

    @Mapping(source = "id", target = "postId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.nickname", target = "nickName")
    @Mapping(target = "title", expression = "java(post.getDisplayTitle())")
    @Mapping(target = "content", expression = "java(post.getDisplayContent())")
    @Mapping(source = "postType.koreanName", target = "postType")
    @Mapping(target = "recommended", constant = "false")
    @Mapping(source = "postImages", target = "images")
    PostDetailResponseDto toPostDetailDto(Post post);

}
