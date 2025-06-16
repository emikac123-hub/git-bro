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

/**
 * {@code ChatGPTClient} is a Spring component responsible for interacting with the OpenAI ChatGPT API.
 * <p>
 * It provides methods to send code diffs or chunks of code to the ChatGPT API and retrieve AI-generated
 * code reviews asynchronously or synchronously.
 * </p>
 * <p>
 * Uses the OkHttp client to handle HTTP requests and Jackson for JSON serialization/deserialization.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGPTClient {

    /**
     * OpenAI API key injected from application properties.
     */
    @Value("${openai.api.key}")
    private String apiKey;

    /**
     * OkHttpClient instance configured with timeouts for connection, write, and read operations.
     */
    OkHttpClient okClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Sends a list of code chunks to the ChatGPT API for synchronous analysis.
     * <p>
     * This method constructs a chat completion request with a system message identifying the role as
     * a senior software engineer reviewing code diffs and appends each chunk as a user message.
     * </p>
     * <p>
     * The method executes the HTTP request synchronously and returns the raw JSON response as a string.
     * </p>
     *
     * @param chunks List of code diff chunks to be reviewed.
     * @return Raw JSON string response from the ChatGPT API.
     * @throws Exception if the HTTP request fails or the API returns an error.
     */
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

        try (Response response = okClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed: " + response.body().string());
            }
            return response.body().string();
        }
    }

    /**
     * Asynchronously analyzes a file diff content using the ChatGPT API.
     * <p>
     * Constructs a detailed prompt that asks ChatGPT to review the Git diff with focus on
     * various quality and security aspects, then sends the request asynchronously.
     * </p>
     * <p>
     * Uses OkHttp's async call mechanism and returns a {@link CompletableFuture} that completes with
     * the extracted textual review content on success or exceptionally on failure.
     * </p>
     *
     * @param filename    The name of the file being analyzed.
     * @param diffContent The git diff content of the file.
     * @return A {@link CompletableFuture} completing with the AI-generated review text.
     */
    public CompletableFuture<String> analyzeFile(String filename, String diffContent) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String prompt = String.format("""
                You are an expert senior Java software engineer specializing in backend services and code parsing.

                Please review the following Git diff from file %s:

                %s

                Focus your review on:

                1. Correctness and efficiency of parsing and filtering operations.
                2. Code style, readability, and maintainability improvements.
                3. Handling of edge cases and robustness against unusual inputs.
                4. Performance considerations and potential optimizations.
                5. Suggestions for unit and integration tests.
                6. Quality of error handling and logging.
                7. Security implications when parsing or processing inputs.

                Lastly, provide a clear final recommendation on whether this pull request should be merged.
                If the issues found are severe, explicitly advise against merging. Always include this recommendation.
                """, filename, diffContent);

        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "model", "gpt-4o",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a expert code reviewer and sytem architect."),
                            Map.of("role", "user", "content", prompt)),
                    "temperature", 0.3));

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
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
