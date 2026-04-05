package com.portfolioBackend.AIChat.text;

import org.springframework.stereotype.Component;

@Component
public class AssistantTextSanitizer {

    public String sanitize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String sanitized = rawText.replace("\r\n", "\n").strip();
        sanitized = sanitized.replaceAll("(?m)^\\s*\\*\\s+", "- ");
        sanitized = sanitized.replaceAll("(?m)^\\s*\\*\\*\\s+", "- ");
        sanitized = sanitized.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        sanitized = sanitized.replaceAll("(?<!\\*)\\*([^\\n*]+)\\*(?!\\*)", "$1");
        sanitized = sanitized.replace("*", "");
        sanitized = sanitized.replaceAll("[ \\t]+\\n", "\n");
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");
        return sanitized.strip();
    }
}
