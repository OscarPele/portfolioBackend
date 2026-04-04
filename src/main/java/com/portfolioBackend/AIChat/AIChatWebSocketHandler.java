package com.portfolioBackend.AIChat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolioBackend.AIChat.service.AIChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class AIChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final AIChatService aiChatService;

    public AIChatWebSocketHandler(ObjectMapper objectMapper, AIChatService aiChatService) {
        this.objectMapper = objectMapper;
        this.aiChatService = aiChatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        AuthenticatedChatUser user = getAuthenticatedUser(session);
        if (user == null) {
            closeUnauthorized(session);
            return;
        }

        aiChatService.registerSession(session, user);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        AuthenticatedChatUser user = getAuthenticatedUser(session);
        if (user == null) {
            closeUnauthorized(session);
            return;
        }

        JsonNode payload = objectMapper.readTree(message.getPayload());
        String type = payload.path("type").asText("");

        if ("subscribe".equals(type)) {
            aiChatService.subscribeToConversation(
                    session,
                    user,
                    payload.path("conversationId").asText("")
            );
            return;
        }

        if ("send_message".equals(type)) {
            aiChatService.handleIncomingMessage(
                    session,
                    user,
                    payload.path("conversationId").asText(""),
                    payload.path("text").asText("")
            );
            return;
        }

        aiChatService.sendError(session, "El evento websocket recibido no es valido.");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        aiChatService.unregisterSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        aiChatService.unregisterSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private AuthenticatedChatUser getAuthenticatedUser(WebSocketSession session) {
        Object rawUser = session.getAttributes().get(ChatHandshakeInterceptor.AUTH_USER_ATTRIBUTE);
        if (rawUser instanceof AuthenticatedChatUser authenticatedChatUser) {
            return authenticatedChatUser;
        }

        return null;
    }

    private void closeUnauthorized(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Token invalido"));
            }
        } catch (Exception ignored) {
            aiChatService.unregisterSession(session);
        }
    }
}
