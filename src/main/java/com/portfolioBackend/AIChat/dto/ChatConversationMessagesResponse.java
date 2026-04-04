package com.portfolioBackend.AIChat.dto;

import java.util.List;

public record ChatConversationMessagesResponse(
        String conversationId,
        List<ChatMessageDto> messages
) {
}
