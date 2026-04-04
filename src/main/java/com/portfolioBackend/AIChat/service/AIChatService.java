package com.portfolioBackend.AIChat.service;

import com.portfolioBackend.AIChat.DeepSeek.DeepSeekChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolioBackend.AIChat.AuthenticatedChatUser;
import com.portfolioBackend.AIChat.dto.ChatConversationMessagesResponse;
import com.portfolioBackend.AIChat.dto.ChatConversationSummaryDto;
import com.portfolioBackend.AIChat.dto.ChatMessageDto;
import com.portfolioBackend.AIChat.model.ChatConversation;
import com.portfolioBackend.AIChat.model.ChatMessage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class AIChatService {

    private final ObjectMapper objectMapper;
    private final DeepSeekChatClient deepSeekChatClient;

    private final Map<String, ChatConversation> conversations = new ConcurrentHashMap<>();
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public AIChatService(ObjectMapper objectMapper, DeepSeekChatClient deepSeekChatClient) {
        this.objectMapper = objectMapper;
        this.deepSeekChatClient = deepSeekChatClient;
    }

    public List<ChatConversationSummaryDto> getConversationsFor(AuthenticatedChatUser viewer) {
        if (viewer.isOscar()) {
            return conversations.values().stream()
                    .sorted((left, right) -> compareInstants(right.lastMessageAt(), left.lastMessageAt()))
                    .map(conversation -> toSummaryDto(conversation, viewer))
                    .toList();
        }

        ChatConversation conversation = ensureConversationFor(viewer);
        return List.of(toSummaryDto(conversation, viewer));
    }

    public ChatConversationMessagesResponse getConversationMessagesFor(
            AuthenticatedChatUser viewer,
            String conversationId
    ) {
        ChatConversation conversation = resolveConversation(viewer, conversationId);
        return new ChatConversationMessagesResponse(
                conversation.id(),
                conversation.snapshotMessages().stream().map(this::toMessageDto).toList()
        );
    }

    public void registerSession(WebSocketSession session, AuthenticatedChatUser user) {
        sessions.put(session.getId(), new SessionContext(session, user));
        sendConversationListToSession(session, user);
    }

    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public void subscribeToConversation(
            WebSocketSession session,
            AuthenticatedChatUser user,
            String conversationId
    ) {
        ChatConversation conversation;

        try {
            conversation = resolveConversation(user, conversationId);
        } catch (ResponseStatusException exception) {
            sendError(session, exception.getReason());
            return;
        }

        conversation.clearUnreadFor(user);
        sendConversationHistory(session, conversation);
        sendConversationListToViewer(user);
    }

    public void handleIncomingMessage(
            WebSocketSession session,
            AuthenticatedChatUser sender,
            String requestedConversationId,
            String rawText
    ) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isBlank()) {
            sendError(session, "No puedes enviar un mensaje vacio.");
            return;
        }

        ChatConversation conversation;

        try {
            if (sender.isOscar()) {
                conversation = resolveConversation(sender, requestedConversationId);
            } else {
                conversation = ensureConversationFor(sender);
            }
        } catch (ResponseStatusException exception) {
            sendError(session, exception.getReason());
            return;
        }

        ChatMessage humanMessage = createHumanMessage(conversation.id(), sender, text);
        conversation.addMessage(humanMessage);

        if (sender.isOscar()) {
            conversation.incrementUnreadForUser();
        } else {
            conversation.incrementUnreadForOscar();
        }

        broadcastMessageCreated(conversation, humanMessage);
        broadcastConversationLists(conversation);

        if (!sender.isOscar()) {
            scheduleAssistantReply(conversation);
        }
    }

    private void scheduleAssistantReply(ChatConversation conversation) {
        List<ChatMessage> contextSnapshot = conversation.snapshotMessages();
        broadcastAssistantPending(conversation);

        CompletableFuture.runAsync(() -> {
            String assistantText = deepSeekChatClient.generateAssistantReply(contextSnapshot);
            ChatMessage assistantMessage = createAssistantMessage(conversation.id(), assistantText);
            conversation.addMessage(assistantMessage);
            conversation.incrementUnreadForUser();
            broadcastMessageCreated(conversation, assistantMessage);
            broadcastConversationLists(conversation);
        }, CompletableFuture.delayedExecutor(250, TimeUnit.MILLISECONDS))
                .whenComplete((ignored, throwable) -> broadcastAssistantIdle(conversation));
    }

    private ChatConversation ensureConversationFor(AuthenticatedChatUser user) {
        String conversationId = buildConversationId(user.uid());
        return conversations.computeIfAbsent(
                conversationId,
                ignored -> new ChatConversation(conversationId, user)
        );
    }

    private ChatConversation resolveConversation(AuthenticatedChatUser viewer, String conversationId) {
        if (!viewer.isOscar()) {
            ChatConversation ownConversation = ensureConversationFor(viewer);
            if (!ownConversation.id().equals(conversationId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes abrir otra conversacion.");
            }
            return ownConversation;
        }

        ChatConversation conversation = conversations.get(conversationId);
        if (conversation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La conversacion no existe.");
        }

        return conversation;
    }

    private String buildConversationId(long uid) {
        return "conversation-" + uid;
    }

    private ChatMessage createHumanMessage(
            String conversationId,
            AuthenticatedChatUser sender,
            String text
    ) {
        String label = sender.isOscar() ? "Oscar" : sender.username();
        return new ChatMessage(
                UUID.randomUUID().toString(),
                conversationId,
                "human",
                sender.username(),
                label,
                text,
                Instant.now()
        );
    }

    private ChatMessage createAssistantMessage(String conversationId, String text) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                conversationId,
                "assistant",
                "assistant",
                "IA de Oscar",
                text,
                Instant.now()
        );
    }

    private void broadcastAssistantPending(ChatConversation conversation) {
        broadcastAssistantState(conversation, "assistant_pending");
    }

    private void broadcastAssistantIdle(ChatConversation conversation) {
        broadcastAssistantState(conversation, "assistant_idle");
    }

    private void broadcastAssistantState(ChatConversation conversation, String type) {
        for (SessionContext context : relevantSessions(conversation)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("conversationId", conversation.id());
            sendEvent(context.session(), payload);
        }
    }

    private void broadcastMessageCreated(ChatConversation conversation, ChatMessage message) {
        for (SessionContext context : relevantSessions(conversation)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "message_created");
            payload.put("conversationId", conversation.id());
            payload.put("conversation", toSummaryDto(conversation, context.user()));
            payload.put("message", toMessageDto(message));
            sendEvent(context.session(), payload);
        }
    }

    private void broadcastConversationLists(ChatConversation conversation) {
        for (SessionContext context : relevantSessions(conversation)) {
            sendConversationListToSession(context.session(), context.user());
        }
    }

    private void sendConversationListToViewer(AuthenticatedChatUser viewer) {
        for (SessionContext context : sessions.values()) {
            if (context.user().uid() == viewer.uid()) {
                sendConversationListToSession(context.session(), context.user());
            }
        }
    }

    private void sendConversationListToSession(WebSocketSession session, AuthenticatedChatUser viewer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "conversation_list");
        payload.put("conversations", getConversationsFor(viewer));
        sendEvent(session, payload);
    }

    private void sendConversationHistory(WebSocketSession session, ChatConversation conversation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "conversation_history");
        payload.put("conversationId", conversation.id());
        payload.put("messages", conversation.snapshotMessages().stream().map(this::toMessageDto).toList());
        sendEvent(session, payload);
    }

    public void sendError(WebSocketSession session, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "error");
        payload.put("code", "CHAT_ERROR");
        payload.put("message", message);
        sendEvent(session, payload);
    }

    private List<SessionContext> relevantSessions(ChatConversation conversation) {
        List<SessionContext> matchingSessions = new ArrayList<>();

        for (SessionContext context : sessions.values()) {
            if (context.user().isOscar() || context.user().uid() == conversation.participant().uid()) {
                matchingSessions.add(context);
            }
        }

        return matchingSessions;
    }

    private ChatConversationSummaryDto toSummaryDto(
            ChatConversation conversation,
            AuthenticatedChatUser viewer
    ) {
        String title = viewer.isOscar() ? "@" + conversation.participant().username() : "Oscar";
        String counterpartUsername = viewer.isOscar()
                ? conversation.participant().username()
                : "oscar";
        String lastMessageAt = conversation.lastMessageAt() == null
                ? null
                : conversation.lastMessageAt().toString();

        return new ChatConversationSummaryDto(
                conversation.id(),
                title,
                counterpartUsername,
                conversation.lastMessagePreview(),
                lastMessageAt,
                conversation.unreadCountFor(viewer)
        );
    }

    private ChatMessageDto toMessageDto(ChatMessage message) {
        return new ChatMessageDto(
                message.id(),
                message.conversationId(),
                message.authorType(),
                message.authorUsername(),
                message.authorLabel(),
                message.text(),
                message.sentAt().toString()
        );
    }

    private void sendEvent(WebSocketSession session, Map<String, Object> payload) {
        if (!session.isOpen()) {
            sessions.remove(session.getId());
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException exception) {
            sessions.remove(session.getId());
        }
    }

    private int compareInstants(Instant left, Instant right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private record SessionContext(WebSocketSession session, AuthenticatedChatUser user) {
    }
}
