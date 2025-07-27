package com.hyetaekon.hyetaekon.comment.mapper;

import com.hyetaekon.hyetaekon.comment.dto.CommentCreateRequestDto;
import com.hyetaekon.hyetaekon.comment.dto.CommentListResponseDto;
import com.hyetaekon.hyetaekon.comment.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    Comment toEntity(CommentCreateRequestDto requestDto);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.nickname", target = "nickname")
    @Mapping(source = "post.id", target = "postId")
    @Mapping(target = "content", expression = "java(comment.getDisplayContent())")
    CommentListResponseDto toResponseDto(Comment comment);

}