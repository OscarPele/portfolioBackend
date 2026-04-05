package com.portfolioBackend.AIChat.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, String> {

    Optional<ChatConversationEntity> findByParticipantUid(long participantUid);
}
