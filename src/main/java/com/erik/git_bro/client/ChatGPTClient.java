package com.erik.git_bro.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

}
