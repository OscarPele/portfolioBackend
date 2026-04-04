package com.portfolioBackend.AIChat;

import com.portfolioBackend.AIChat.dto.ChatConversationMessagesResponse;
import com.portfolioBackend.AIChat.dto.ChatConversationSummaryDto;
import com.portfolioBackend.AIChat.service.AIChatService;
import com.portfolioBackend.security.JwtUtils;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ai-chat")
public class AIChatController {

    private final AIChatService aiChatService;

    public AIChatController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/conversations")
    public List<ChatConversationSummaryDto> getConversations(@AuthenticationPrincipal Jwt jwt) {
        return aiChatService.getConversationsFor(toViewer(jwt));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ChatConversationMessagesResponse getConversationMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId
    ) {
        return aiChatService.getConversationMessagesFor(toViewer(jwt), conversationId);
    }

    private AuthenticatedChatUser toViewer(Jwt jwt) {
        Long uid = JwtUtils.getUid(jwt);
        String username = jwt != null ? jwt.getSubject() : null;

        if (uid == null || username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT invalido para AIChat.");
        }

        return new AuthenticatedChatUser(uid, username);
    }
}
