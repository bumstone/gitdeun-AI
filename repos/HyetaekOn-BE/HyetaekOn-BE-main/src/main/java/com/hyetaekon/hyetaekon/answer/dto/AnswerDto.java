package com.hyetaekon.hyetaekon.answer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnswerDto {
    private Long id;
    private Long postId;
    private Long userId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private boolean selected;
}
