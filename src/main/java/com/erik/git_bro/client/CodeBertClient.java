package com.erik.git_bro.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * {@code CodeBertClient} is a Spring component responsible for interacting with the Hugging Face CodeBERT model API.
 * <p>
 * It provides functionality to send code snippets to the CodeBERT inference endpoint
 * and retrieve AI-generated analysis or embeddings synchronously.
 * </p>
 * <p>
 * Uses OkHttp for HTTP requests and Jackson for JSON serialization/deserialization.
 * </p>
 */
@Component
@Slf4j
public class CodeBertClient  {

    /**
     * Hugging Face API token for authenticating requests.
     * Injected from application properties.
     */
    @Value("${huggingface.api.token}")
    private String huggingfaceToken;

    /**
     * OkHttpClient instance used for HTTP communication.
     */
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Jackson ObjectMapper instance for JSON serialization and deserialization.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The Hugging Face inference API URL for the CodeBERT base model.
     */
    private static final String API_URL = "https://api-inference.huggingface.co/models/microsoft/codebert-base";

    /**
     * Analyzes the given list of code snippets by sending them to the CodeBERT Hugging Face inference API.
     * <p>
     * Constructs a JSON payload with the code snippets, sends an HTTP POST request with the Hugging Face API token,
     * and returns the raw JSON response from the API as a string.
     * </p>
     *
     * @param codeSnippet List of code snippet strings to analyze.
     * @return Raw JSON response from the CodeBERT model as a String.
     * @throws IOException if the HTTP request fails or the API returns an error response.
     */
    public String analyzeCode(List<String> codeSnippet) throws IOException {
        Map<String, String> payloadMap = Map.of("inputs", objectMapper.writeValueAsString(codeSnippet));
        String jsonPayload = new ObjectMapper().writeValueAsString(payloadMap);
        final var body = RequestBody.create(
                jsonPayload,
                MediaType.parse("application/json"));

        final Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + this.huggingfaceToken)
                .post(body)
                .build();

        log.info("Here is the Request: {}", request.body());
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error("CodeBERT API request failed with code {}: {}", response.code(), errorBody);
                throw new IOException("API request failed with code " + response.code() + ": " + errorBody);
            }
            String responseBody = response.body().string();
            log.debug("Received CodeBERT response, size: {} characters", responseBody.length());
            return responseBody;
        }
    }

    /**
     * Lifecycle callback that logs the Hugging Face API token once the component is constructed.
     * Useful for verifying configuration during application startup.
     */
    @PostConstruct
    public void init() {
        System.out.println("Loaded HuggingFace Token: " + huggingfaceToken);
    }
}
