package com.erik.git_bro.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.erik.git_bro.util.Prompts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * {@code ChatGPTClient} is a Spring component responsible for interacting with
 * the OpenAI ChatGPT API.
 * <p>
 * It provides methods to send code diffs or chunks of code to the ChatGPT API
 * and retrieve AI-generated
 * code reviews asynchronously or synchronously.
 * </p>
 * <p>
 * Uses the OkHttp client to handle HTTP requests and Jackson for JSON
 * serialization/deserialization.
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
     * OkHttpClient instance configured with timeouts for connection, write, and
     * read operations.
     */
    OkHttpClient okClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Asynchronously analyzes a file diff content using the ChatGPT API.
     * <p>
     * Constructs a detailed prompt that asks ChatGPT to review the Git diff with
     * focus on
     * various quality and security aspects, then sends the request asynchronously.
     * </p>
     * <p>
     * Uses OkHttp's async call mechanism and returns a {@link CompletableFuture}
     * that completes with
     * the extracted textual review content on success or exceptionally on failure.
     * </p>
     *
     * @param filename    The name of the file being analyzed.
     * @param diffContent The git diff content of the file.
     * @return A {@link CompletableFuture} completing with the AI-generated review
     *         text.
     */
    public CompletableFuture<String> analyzeFile(String filename, String diffContent) {
        String prompt = Prompts.getAnalysisPrompt(filename, diffContent);
        return (CompletableFuture<String>) this.sendOffPromptToOpenAI(prompt);
    }

    public CompletableFuture<String> analyzeFileLineByLine(String filename, String diffContent) {
        final String prompt = Prompts.getAnalysisPrompt(filename, diffContent);
        return (CompletableFuture<String>) this.sendOffPromptToOpenAI(prompt);
    }

    private CompletableFuture<?> sendOffPromptToOpenAI(final String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
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
                    .header("Authorization", "Bearer " + this.apiKey)
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
                        log.info("The response====");
                        log.info(responseText);
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

    public CompletableFuture<String> sendJavaDocPrompt(String methodSource) {
        String prompt = String.format(
                """
                        Generate a professional JavaDoc comment for the following Java method:

                        %s

                        - Follow JavaDoc conventions
                        - Document parameters and return value
                        - Do not include extra explanations, only the JavaDoc block

                        """, methodSource);

        return (CompletableFuture<String>) this.sendOffPromptToOpenAI(prompt);
    }

}
