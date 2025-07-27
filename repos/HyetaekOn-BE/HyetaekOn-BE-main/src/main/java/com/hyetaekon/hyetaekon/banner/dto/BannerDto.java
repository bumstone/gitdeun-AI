package com.hyetaekon.hyetaekon.banner.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BannerDto {
    private Long id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private int displayOrder;
    private LocalDateTime createdAt;
}
