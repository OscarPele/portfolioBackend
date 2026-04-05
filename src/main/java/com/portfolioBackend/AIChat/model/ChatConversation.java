package com.portfolioBackend.AIChat.model;

import com.portfolioBackend.AIChat.AuthenticatedChatUser;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChatConversation {

    private final String id;
    private final AuthenticatedChatUser participant;
    private final CopyOnWriteArrayList<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private final AtomicInteger unreadForOscar = new AtomicInteger();
    private final AtomicInteger unreadForUser = new AtomicInteger();

    private volatile Instant lastMessageAt;

    public ChatConversation(String id, AuthenticatedChatUser participant) {
        this.id = id;
        this.participant = participant;
    }

    public ChatConversation(
            String id,
            AuthenticatedChatUser participant,
            List<ChatMessage> initialMessages,
            int unreadForOscar,
            int unreadForUser,
            Instant lastMessageAt
    ) {
        this.id = id;
        this.participant = participant;
        this.messages.addAll(initialMessages);
        this.unreadForOscar.set(Math.max(unreadForOscar, 0));
        this.unreadForUser.set(Math.max(unreadForUser, 0));
        this.lastMessageAt = lastMessageAt != null
                ? lastMessageAt
                : (initialMessages.isEmpty() ? null : initialMessages.getLast().sentAt());
    }

    public String id() {
        return id;
    }

    public AuthenticatedChatUser participant() {
        return participant;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        lastMessageAt = message.sentAt();
    }

    public List<ChatMessage> snapshotMessages() {
        return List.copyOf(messages);
    }

    public Instant lastMessageAt() {
        return lastMessageAt;
    }

    public String lastMessagePreview() {
        if (messages.isEmpty()) {
            return "";
        }

        String preview = messages.getLast().text();
        if (preview.length() <= 90) {
            return preview;
        }

        return preview.substring(0, 87) + "...";
    }

    public int unreadCountFor(AuthenticatedChatUser viewer) {
        return viewer.isOscar() ? unreadForOscar.get() : unreadForUser.get();
    }

    public void clearUnreadFor(AuthenticatedChatUser viewer) {
        if (viewer.isOscar()) {
            unreadForOscar.set(0);
            return;
        }

        unreadForUser.set(0);
    }

    public void incrementUnreadForOscar() {
        unreadForOscar.incrementAndGet();
    }

    public void incrementUnreadForUser() {
        unreadForUser.incrementAndGet();
    }

    public int unreadForOscar() {
        return unreadForOscar.get();
    }

    public int unreadForUser() {
        return unreadForUser.get();
    }
}
