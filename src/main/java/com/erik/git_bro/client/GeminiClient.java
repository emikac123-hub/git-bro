package com.erik.git_bro.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.erik.git_bro.util.API;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    OkHttpClient okClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = API.GEMINI_URL;

    public CompletableFuture<?> analyzeFileLineByLine(String filename, String diffContent) {
        final String prompt = Prompts.getAnalysisPrompt(filename, diffContent);
        return this.sendPromptToGemini(prompt);
    }

    private CompletableFuture<String> sendPromptToGemini(final String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            ));

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL + "?key=" + this.apiKey)
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
                                    new RuntimeException("Unsuccessful response: " + response.code() + " " + response.body().string()));
                            return;
                        }

                        String responseText = responseBody.string();
                        JsonNode jsonNode = objectMapper.readTree(responseText);
                        log.info("The response====");
                        log.info(responseText);
                        String content = jsonNode
                                .get("candidates")
                                .get(0)
                                .get("content")
                                .get("parts")
                                .get(0)
                                .get("text")
                                .asText();

                        future.complete(content);
                    } catch (Exception e) {
                        future.completeExceptionally(
                                new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e));
                    }
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException("Failed to build request: " + e.getMessage(), e));
        }

        return future;
    }
}
