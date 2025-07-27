package com.hyetaekon.hyetaekon.chatbot.service;

import com.hyetaekon.hyetaekon.chatbot.dto.ChatbotDto;
import com.hyetaekon.hyetaekon.chatbot.entity.Chatbot;
import com.hyetaekon.hyetaekon.chatbot.mapper.ChatbotMapper;
import com.hyetaekon.hyetaekon.chatbot.repository.ChatbotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private final ChatbotRepository chatbotRepository;

    // 📌 질문을 DB에서 찾아서 답변을 반환하는 메서드
    public ChatbotDto getAnswer(String question) {
        Optional<Chatbot> chatbot = chatbotRepository.findByQuestion(question);

        // 📌 질문이 DB에 있다면 해당 답변 반환
        if (chatbot.isPresent()) {
            return ChatbotMapper.INSTANCE.toDto(chatbot.get());
        }

        // 📌 질문이 DB에 없을 경우 기본 응답 반환
        return new ChatbotDto(question, "죄송해요, 해당 질문에 대한 답변을 찾을 수 없어요.", null);
    }

    // 📌 새로운 질문-답변을 DB에 추가하는 메서드
    public ChatbotDto addQuestionAndAnswer(ChatbotDto chatbotDto) {
        Chatbot chatbot = ChatbotMapper.INSTANCE.toEntity(chatbotDto);
        chatbotRepository.save(chatbot);
        return chatbotDto;
    }
}
