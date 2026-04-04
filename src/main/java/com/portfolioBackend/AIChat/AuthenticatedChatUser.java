package com.portfolioBackend.AIChat;

public record AuthenticatedChatUser(long uid, String username) {

    public boolean isOscar() {
        return "oscar".equalsIgnoreCase(username);
    }
}
