package com.portfolioBackend.AIChat.dto;

public record ChatMessageDto(
        String id,
        String conversationId,
        String authorType,
        String authorUsername,
        String authorLabel,
        String text,
        String sentAt
) {
}
