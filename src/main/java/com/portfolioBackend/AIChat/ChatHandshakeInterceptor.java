package com.portfolioBackend.AIChat;

import com.portfolioBackend.security.JwtUtils;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AUTH_USER_ATTRIBUTE = "authenticatedChatUser";

    private final JwtDecoder jwtDecoder;

    public ChatHandshakeInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token == null || token.isBlank()) {
            reject(response);
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            Long uid = JwtUtils.getUid(jwt);
            String username = jwt.getSubject();

            if (uid == null || username == null || username.isBlank()) {
                reject(response);
                return false;
            }

            attributes.put(AUTH_USER_ATTRIBUTE, new AuthenticatedChatUser(uid, username));
            return true;
        } catch (JwtException exception) {
            reject(response);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private void reject(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.getServletResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
