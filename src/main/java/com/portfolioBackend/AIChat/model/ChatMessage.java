package com.portfolioBackend.AIChat.model;

import java.time.Instant;

public record ChatMessage(
        String id,
        String conversationId,
        String authorType,
        String authorUsername,
        String authorLabel,
        String text,
        Instant sentAt
) {
}
