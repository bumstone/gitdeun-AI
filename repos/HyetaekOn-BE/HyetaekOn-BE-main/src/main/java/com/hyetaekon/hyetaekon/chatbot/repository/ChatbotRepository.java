package com.hyetaekon.hyetaekon.chatbot.repository;

import com.hyetaekon.hyetaekon.chatbot.entity.Chatbot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatbotRepository extends JpaRepository<Chatbot, Long> {
    Optional<Chatbot> findByQuestion(String question);
}
