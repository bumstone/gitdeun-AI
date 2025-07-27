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
public class PostCreateRequestDto {
    private String title;
    private String content;
    private String postType;
    private String urlTitle;
    private String urlPath;
    private String tags;
    private List<MultipartFile> images;  // 이미지 파일
}
