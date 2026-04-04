package com.portfolioBackend.AIChat.DeepSeek;

import java.util.List;

public record DeepSeekChatCompletionResponse(List<Choice> choices) {

    public record Choice(Message message) {
    }

    public record Message(String role, String content) {
    }
}
