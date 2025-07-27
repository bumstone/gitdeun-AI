package com.hyetaekon.hyetaekon.chatbot.mapper;

import com.hyetaekon.hyetaekon.chatbot.dto.ChatbotDto;
import com.hyetaekon.hyetaekon.chatbot.entity.Chatbot;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ChatbotMapper {
    ChatbotMapper INSTANCE = Mappers.getMapper(ChatbotMapper.class);

    ChatbotDto toDto(Chatbot chatbot);
    Chatbot toEntity(ChatbotDto chatbotDto);
}
