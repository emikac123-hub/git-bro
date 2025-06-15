package com.erik.git_bro.client;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Component
public class ChatGPTClient {

    @Value("${openai.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public String analyzeCode(String diffChunk) throws Exception {

        final var extractedChunk = this.extractInput(diffChunk);
        log.info("CHUNK");
        log.info(extractedChunk);
        String payloadTemplate = """
                    {
                    "model": "gpt-4o",
                    "messages": [
                          {"role": "system", "content": "You are a senior software engineer reviewing code diffs."},
                          {"role": "user", "content": "Please review the following diff and give concise feedback: %s"}
                    ],
                    "temperature": 0.2
                    }
                """;

        final String payload = String.format(payloadTemplate, extractedChunk);

        RequestBody body = RequestBody.create(payload, MediaType.get("application/json"));

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

    public String extractInput(String jsonString) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonString);
        return rootNode.get("input").asText();
    }

}
