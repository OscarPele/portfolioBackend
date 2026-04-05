package com.portfolioBackend.AIChat.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_chat_conversations")
public class ChatConversationEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @Column(name = "participant_uid", nullable = false, unique = true)
    private long participantUid;

    @Column(name = "participant_username", nullable = false, length = 100)
    private String participantUsername;

    @Column(name = "unread_for_oscar", nullable = false)
    private int unreadForOscar;

    @Column(name = "unread_for_user", nullable = false)
    private int unreadForUser;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ai_chat_messages",
            joinColumns = @JoinColumn(name = "conversation_id")
    )
    @OrderColumn(name = "message_order")
    private List<StoredChatMessageEmbeddable> messages = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getParticipantUid() {
        return participantUid;
    }

    public void setParticipantUid(long participantUid) {
        this.participantUid = participantUid;
    }

    public String getParticipantUsername() {
        return participantUsername;
    }

    public void setParticipantUsername(String participantUsername) {
        this.participantUsername = participantUsername;
    }

    public int getUnreadForOscar() {
        return unreadForOscar;
    }

    public void setUnreadForOscar(int unreadForOscar) {
        this.unreadForOscar = unreadForOscar;
    }

    public int getUnreadForUser() {
        return unreadForUser;
    }

    public void setUnreadForUser(int unreadForUser) {
        this.unreadForUser = unreadForUser;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public List<StoredChatMessageEmbeddable> getMessages() {
        return messages;
    }

    public void setMessages(List<StoredChatMessageEmbeddable> messages) {
        this.messages = messages;
    }
}
