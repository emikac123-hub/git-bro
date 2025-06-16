package com.erik.git_bro.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    OkHttpClient okClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public CompletableFuture<String> analyzeFile(String filename, String diffContent) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(String.format(
                    "Please review the following Git diff from file %s:\n\n%s\n\n",
                    filename,
                    diffContent));
            promptBuilder.append("Additional Instructions:\n");
            promptBuilder.append("- Evaluate code style and adherence to best practices.\n");
            promptBuilder.append("- Identify any potential performance bottlenecks.\n");
            promptBuilder.append(
                    "- Give a final recommendation if the code should be merged in. DO NOT recommend code be merged in if there are security issues.");
            if (filename.endsWith(".java")) {
                promptBuilder.append("- Ensure compliance with Java coding standards (e.g., naming conventions).\n");
            }
            RequestBody body = RequestBody.create(
                    diffContent,
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            this.okClient.newCall(request).enqueue(new Callback() {
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
