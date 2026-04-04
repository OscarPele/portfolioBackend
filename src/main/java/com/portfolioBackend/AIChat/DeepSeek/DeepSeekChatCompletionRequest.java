package com.portfolioBackend.AIChat.DeepSeek;

import java.util.List;

public record DeepSeekChatCompletionRequest(
        String model,
        List<DeepSeekChatMessage> messages,
        boolean stream
) {
}
