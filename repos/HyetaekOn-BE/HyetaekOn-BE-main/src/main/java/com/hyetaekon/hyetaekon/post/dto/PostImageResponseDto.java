package com.hyetaekon.hyetaekon.post.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageResponseDto {
    private Long imageId;          // 이미지 ID
    private String imageUrl;  // 이미지 URL
}
