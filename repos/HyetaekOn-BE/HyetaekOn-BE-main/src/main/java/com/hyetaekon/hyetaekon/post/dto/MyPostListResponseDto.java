package com.hyetaekon.hyetaekon.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPostListResponseDto {
    private Long postId;
    private String title;
    private String content;
    private String nickName;  // 작성자 닉네임
    private LocalDateTime createdAt;
    private int recommendCnt;
    private int commentCnt;
    private int viewCnt;
}
