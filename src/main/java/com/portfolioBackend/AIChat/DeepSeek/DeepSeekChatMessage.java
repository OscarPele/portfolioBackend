package com.portfolioBackend.AIChat.DeepSeek;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeepSeekChatMessage(String role, String content, String name) {
}
