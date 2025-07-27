package com.hyetaekon.hyetaekon.chatbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Chatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String question;  // 질문

    @Column(nullable = false, length = 1000)
    private String answer;  // 답변

    public Chatbot(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
}
