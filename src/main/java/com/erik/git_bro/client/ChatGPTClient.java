package com.erik.git_bro.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.tags.HtmlEscapeTag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGPTClient {

    @Value("${openai.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";


    public String analyzeCode(List<String> chunks) throws Exception {
        List<ChatMessage> messages = new ArrayList<>();

        OkHttpClient okClient = new OkHttpClient();
        log.info("The chunks: {}", chunks);
        // Add the system message
        messages.add(new ChatMessage("system", "You are a senior software engineer reviewing code diffs."));

        // Add each diff chunk as a separate user message
        for (String chunk : chunks) {
            messages.add(new ChatMessage("user", "Please review this diff! :\n" + chunk));
        }

        // Build the request body map
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("model", "gpt-4o");
        payloadMap.put("temperature", 0.2);
        payloadMap.put("messages", messages);

        // Serialize to JSON
        String payloadJson = objectMapper.writeValueAsString(payloadMap);

        log.info("The payload: {}", payloadMap);
        RequestBody body = RequestBody.create(payloadJson, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed: " + response.body().string());
            }
            return response.body().string();
        }
    }

    public CompletableFuture<String> analyzeFile(String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "model", "gpt-4o",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a senior code reviewer."),
                            Map.of("role", "user", "content", prompt)),
                    "temperature", 0.2));

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            this.client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(new RuntimeException("HTTP request failed: " + e.getMessage(), e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful() || responseBody == null) {
                            future.completeExceptionally(
                                    new RuntimeException("Unsuccessful response: " + response.code()));
                            return;
                        }

                        String responseText = responseBody.string();
                        JsonNode jsonNode = objectMapper.readTree(responseText);
                        String content = jsonNode
                                .get("choices")
                                .get(0)
                                .get("message")
                                .get("content")
                                .asText();

                        future.complete(content);
                    } catch (Exception e) {
                        future.completeExceptionally(
                                new RuntimeException("Failed to parse OpenAI response: " + e.getMessage(), e));
                    }
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException("Failed to build request: " + e.getMessage(), e));
        }

        return future;
    }

}
