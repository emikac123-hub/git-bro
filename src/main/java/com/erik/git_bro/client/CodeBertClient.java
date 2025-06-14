package com.erik.git_bro.client;

import java.io.IOException;

import org.springframework.stereotype.Component;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class CodeBertClient {
    private final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = "https://api-inference.huggingface.co/models/microsoft/codebert-base";
    private static final String API_TOKEN = "";

    public String analyzeCode(String codeSnippet) throws IOException {
        final var body = RequestBody.create(
            "{\"inputs\": \"" + codeSnippet + "\"}",
            MediaType.parse("application/json")
        );

        final Request request = new Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer " + API_TOKEN)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if(!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
}


