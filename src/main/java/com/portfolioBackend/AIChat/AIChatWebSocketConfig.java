package com.portfolioBackend.AIChat;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class AIChatWebSocketConfig implements WebSocketConfigurer {

    private final AIChatWebSocketHandler aiChatWebSocketHandler;
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;

    public AIChatWebSocketConfig(
            AIChatWebSocketHandler aiChatWebSocketHandler,
            ChatHandshakeInterceptor chatHandshakeInterceptor
    ) {
        this.aiChatWebSocketHandler = aiChatWebSocketHandler;
        this.chatHandshakeInterceptor = chatHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiChatWebSocketHandler, "/ws/ai-chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "https://oscarpelegrina.com",
                        "https://www.oscarpelegrina.com"
                )
                .setAllowedOriginPatterns("http://192.168.*.*:5173", "http://192.168.*.*:3000");
    }
}
