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

@Component
@Slf4j
public class CodeBertClient  {
    @Value("${huggingface.api.token}")
    private String huggingfaceToken;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "https://api-inference.huggingface.co/models/microsoft/codebert-base";
 


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


    @PostConstruct
    public void init() {
        System.out.println("Loaded HuggingFace Token: " + huggingfaceToken);
    }
}
