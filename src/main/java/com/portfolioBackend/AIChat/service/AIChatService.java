package com.portfolioBackend.AIChat.service;

import com.portfolioBackend.AIChat.DeepSeek.DeepSeekChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolioBackend.AIChat.AuthenticatedChatUser;
import com.portfolioBackend.AIChat.dto.ChatConversationMessagesResponse;
import com.portfolioBackend.AIChat.dto.ChatConversationSummaryDto;
import com.portfolioBackend.AIChat.dto.ChatMessageDto;
import com.portfolioBackend.AIChat.model.ChatConversation;
import com.portfolioBackend.AIChat.model.ChatMessage;
import com.portfolioBackend.AIChat.persistence.ChatConversationEntity;
import com.portfolioBackend.AIChat.persistence.ChatConversationRepository;
import com.portfolioBackend.AIChat.persistence.StoredChatMessageEmbeddable;
import com.portfolioBackend.AIChat.text.AssistantTextSanitizer;
import com.portfolioBackend.notifications.OwnerAlertService;
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

    private static final String INITIAL_GREETING = """
            Hola, soy la IA de Oscar.

            Puedes preguntarme algo concreto sobre su perfil, sus proyectos, su stack o su forma de trabajar. Si prefieres, tambien puedo contarte algo interesante sobre el.

            Si quieres hablar directamente con Oscar, puedes dejar tu mensaje en este chat y esperar a que responda personalmente, o escribirle a oscarpelegrina99@gmail.com.
            """;

    private final ObjectMapper objectMapper;
    private final DeepSeekChatClient deepSeekChatClient;
    private final OwnerAlertService ownerAlertService;
    private final ChatConversationRepository chatConversationRepository;
    private final AssistantTextSanitizer assistantTextSanitizer;

    private final Map<String, ChatConversation> conversations = new ConcurrentHashMap<>();
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public AIChatService(
            ObjectMapper objectMapper,
            DeepSeekChatClient deepSeekChatClient,
            OwnerAlertService ownerAlertService,
            ChatConversationRepository chatConversationRepository,
            AssistantTextSanitizer assistantTextSanitizer
    ) {
        this.objectMapper = objectMapper;
        this.deepSeekChatClient = deepSeekChatClient;
        this.ownerAlertService = ownerAlertService;
        this.chatConversationRepository = chatConversationRepository;
        this.assistantTextSanitizer = assistantTextSanitizer;
    }

    public List<ChatConversationSummaryDto> getConversationsFor(AuthenticatedChatUser viewer) {
        if (viewer.isOscar()) {
            return loadPersistedConversations().stream()
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

        synchronized (conversation) {
            conversation.clearUnreadFor(user);
            persistConversation(conversation);
        }
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

        ChatMessage humanMessage;
        synchronized (conversation) {
            humanMessage = createHumanMessage(conversation.id(), sender, text);
            conversation.addMessage(humanMessage);

            if (sender.isOscar()) {
                conversation.incrementUnreadForUser();
            } else {
                conversation.incrementUnreadForOscar();
            }

            persistConversation(conversation);
        }

        broadcastMessageCreated(conversation, humanMessage);
        broadcastConversationLists(conversation);

        if (!sender.isOscar()) {
            ownerAlertService.notifyChatMessage(sender.uid(), text);
            scheduleAssistantReply(conversation);
        }
    }

    private void scheduleAssistantReply(ChatConversation conversation) {
        List<ChatMessage> contextSnapshot;
        synchronized (conversation) {
            contextSnapshot = conversation.snapshotMessages();
        }
        broadcastAssistantPending(conversation);

        CompletableFuture.runAsync(() -> {
            String assistantText = deepSeekChatClient.generateAssistantReply(contextSnapshot);
            ChatMessage assistantMessage;
            synchronized (conversation) {
                assistantMessage = createAssistantMessage(conversation.id(), assistantText);
                conversation.addMessage(assistantMessage);
                conversation.incrementUnreadForUser();
                persistConversation(conversation);
            }
            broadcastMessageCreated(conversation, assistantMessage);
            broadcastConversationLists(conversation);
        }, CompletableFuture.delayedExecutor(250, TimeUnit.MILLISECONDS))
                .whenComplete((ignored, throwable) -> broadcastAssistantIdle(conversation));
    }

    private ChatConversation ensureConversationFor(AuthenticatedChatUser user) {
        String conversationId = buildConversationId(user.uid());
        return conversations.computeIfAbsent(conversationId, ignored -> loadOrCreateConversation(conversationId, user));
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
            conversation = chatConversationRepository.findById(conversationId)
                    .map(entity -> conversations.computeIfAbsent(conversationId, ignored -> toConversation(entity)))
                    .orElse(null);
        }
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

    private ChatConversation createConversationWithGreeting(String conversationId, AuthenticatedChatUser user) {
        ChatConversation conversation = new ChatConversation(conversationId, user);
        conversation.addMessage(createAssistantMessage(conversationId, INITIAL_GREETING));
        persistConversation(conversation);
        return conversation;
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
        String lastMessagePreview = buildLastMessagePreview(conversation);

        return new ChatConversationSummaryDto(
                conversation.id(),
                title,
                counterpartUsername,
                lastMessagePreview,
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
                sanitizeMessageText(message),
                message.sentAt().toString()
        );
    }

    private List<ChatConversation> loadPersistedConversations() {
        return chatConversationRepository.findAll().stream()
                .map(entity -> conversations.computeIfAbsent(entity.getId(), ignored -> toConversation(entity)))
                .sorted((left, right) -> compareInstants(right.lastMessageAt(), left.lastMessageAt()))
                .toList();
    }

    private ChatConversation loadOrCreateConversation(String conversationId, AuthenticatedChatUser user) {
        return chatConversationRepository.findById(conversationId)
                .map(this::toConversation)
                .orElseGet(() -> createConversationWithGreeting(conversationId, user));
    }

    private void persistConversation(ChatConversation conversation) {
        chatConversationRepository.save(toEntity(conversation));
    }

    private ChatConversationEntity toEntity(ChatConversation conversation) {
        ChatConversationEntity entity = new ChatConversationEntity();
        entity.setId(conversation.id());
        entity.setParticipantUid(conversation.participant().uid());
        entity.setParticipantUsername(conversation.participant().username());
        entity.setUnreadForOscar(conversation.unreadForOscar());
        entity.setUnreadForUser(conversation.unreadForUser());
        entity.setLastMessageAt(conversation.lastMessageAt());
        entity.setMessages(new ArrayList<>(conversation.snapshotMessages().stream()
                .map(this::toStoredMessage)
                .toList()));
        return entity;
    }

    private StoredChatMessageEmbeddable toStoredMessage(ChatMessage message) {
        StoredChatMessageEmbeddable storedMessage = new StoredChatMessageEmbeddable();
        storedMessage.setId(message.id());
        storedMessage.setAuthorType(message.authorType());
        storedMessage.setAuthorUsername(message.authorUsername());
        storedMessage.setAuthorLabel(message.authorLabel());
        storedMessage.setText(message.text());
        storedMessage.setSentAt(message.sentAt());
        return storedMessage;
    }

    private ChatConversation toConversation(ChatConversationEntity entity) {
        List<ChatMessage> messages = entity.getMessages().stream()
                .map(storedMessage -> toModelMessage(entity.getId(), storedMessage))
                .toList();

        return new ChatConversation(
                entity.getId(),
                new AuthenticatedChatUser(entity.getParticipantUid(), entity.getParticipantUsername()),
                messages,
                entity.getUnreadForOscar(),
                entity.getUnreadForUser(),
                entity.getLastMessageAt()
        );
    }

    private ChatMessage toModelMessage(String conversationId, StoredChatMessageEmbeddable storedMessage) {
        String text = "assistant".equalsIgnoreCase(storedMessage.getAuthorType())
                ? assistantTextSanitizer.sanitize(storedMessage.getText())
                : storedMessage.getText();

        return new ChatMessage(
                storedMessage.getId(),
                conversationId,
                storedMessage.getAuthorType(),
                storedMessage.getAuthorUsername(),
                storedMessage.getAuthorLabel(),
                text,
                storedMessage.getSentAt()
        );
    }

    private String buildLastMessagePreview(ChatConversation conversation) {
        List<ChatMessage> messages = conversation.snapshotMessages();
        if (messages.isEmpty()) {
            return "";
        }

        String preview = sanitizeMessageText(messages.getLast());
        if (preview.length() <= 90) {
            return preview;
        }

        return preview.substring(0, 87) + "...";
    }

    private String sanitizeMessageText(ChatMessage message) {
        if ("assistant".equalsIgnoreCase(message.authorType())) {
            return assistantTextSanitizer.sanitize(message.text());
        }

        return message.text();
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
