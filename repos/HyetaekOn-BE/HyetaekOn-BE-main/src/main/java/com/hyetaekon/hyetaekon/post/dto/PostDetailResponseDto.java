package com.hyetaekon.hyetaekon.post.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailResponseDto {
    private Long postId;
    private String nickName;  // 작성자 닉네임
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String postType;
    private int recommendCnt;
    private int viewCnt;
    private String urlTitle;
    private String urlPath;
    private String tags;
    private List<PostImageResponseDto> images;
    private boolean recommended; // 현재 로그인한 사용자의 추천 여부

}
