package com.portfolioBackend.AIChat.DeepSeek;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolioBackend.AIChat.model.ChatMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekChatClient {

    private static final String MISSING_API_KEY_MESSAGE =
            "Placeholder: falta configurar APP_DEEPSEEK_API_KEY en el backend para activar DeepSeek.";

    private static final String FALLBACK_ERROR_MESSAGE =
            "Placeholder: DeepSeek no pudo generar respuesta en este momento. Mas adelante afinaremos el prompt y el manejo de errores.";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekChatClient(
            ObjectMapper objectMapper,
            @Value("${app.deepseek.api-key:}") String apiKey,
            @Value("${app.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${app.deepseek.model:deepseek-chat}") String model
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String generateAssistantReply(List<ChatMessage> conversationMessages) {
        if (apiKey == null || apiKey.isBlank()) {
            return MISSING_API_KEY_MESSAGE;
        }

        try {
            DeepSeekChatCompletionRequest payload = new DeepSeekChatCompletionRequest(
                    model,
                    mapConversation(conversationMessages),
                    false
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return FALLBACK_ERROR_MESSAGE;
            }

            DeepSeekChatCompletionResponse completionResponse = objectMapper.readValue(
                    response.body(),
                    DeepSeekChatCompletionResponse.class
            );

            if (completionResponse.choices() == null || completionResponse.choices().isEmpty()) {
                return FALLBACK_ERROR_MESSAGE;
            }

            DeepSeekChatCompletionResponse.Message message = completionResponse.choices().getFirst().message();
            if (message == null || message.content() == null || message.content().isBlank()) {
                return FALLBACK_ERROR_MESSAGE;
            }

            return message.content().trim();
        } catch (Exception exception) {
            return FALLBACK_ERROR_MESSAGE;
        }
    }

    private List<DeepSeekChatMessage> mapConversation(List<ChatMessage> conversationMessages) {
        List<DeepSeekChatMessage> mappedMessages = new ArrayList<>();

        for (ChatMessage message : conversationMessages) {
            mappedMessages.add(new DeepSeekChatMessage(resolveRole(message), message.text()));
        }

        return mappedMessages;
    }

    private String resolveRole(ChatMessage message) {
        if ("assistant".equalsIgnoreCase(message.authorType())) {
            return "assistant";
        }

        if ("oscar".equalsIgnoreCase(message.authorUsername())) {
            return "assistant";
        }

        return "user";
    }
}
