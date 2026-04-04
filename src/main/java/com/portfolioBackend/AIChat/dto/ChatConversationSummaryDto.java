package com.portfolioBackend.AIChat.dto;

public record ChatConversationSummaryDto(
        String id,
        String title,
        String counterpartUsername,
        String lastMessagePreview,
        String lastMessageAt,
        int unreadCount
) {
}
