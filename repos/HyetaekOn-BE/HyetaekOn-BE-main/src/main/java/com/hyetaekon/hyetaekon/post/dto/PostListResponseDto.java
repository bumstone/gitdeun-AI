package com.hyetaekon.hyetaekon.post.dto;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostListResponseDto {
    private Long postId;
    private String title;
    private String nickName;  // 작성자 닉네임
    private Long userId;
    private LocalDateTime createdAt;
    private int viewCnt;
    private String postType;
    private int recommendCnt;
}
