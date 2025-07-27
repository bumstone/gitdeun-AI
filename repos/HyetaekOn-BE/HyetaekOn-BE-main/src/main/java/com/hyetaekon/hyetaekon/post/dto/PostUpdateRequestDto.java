package com.hyetaekon.hyetaekon.post.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequestDto {
    private String title;
    private String content;
    private String postType;
    private String urlTitle;
    private String urlPath;
    private String tags;

    private List<Long> keepImageIds;                 // 유지할 기존 이미지 ID
    private List<MultipartFile> newImages;           // 새로 추가할 이미지
}
