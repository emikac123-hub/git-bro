package com.erik.git_bro.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.erik.git_bro.client.ChatGPTClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;

@Component("chatGptAnalyzer")
public class ChatGptAnalyzer implements CodeAnalyzer {

    private final ChatGPTClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();



    public ChatGptAnalyzer(ChatGPTClient client) {
        this.client = client;
    }

    @Override
    public String parseAiResponse(String aiJsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(aiJsonResponse);
            JsonNode content = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content");

            if (content.isMissingNode() || content.isNull()) {
                throw new Exception(
                        "The AI response did not contain a 'message.content' field. Please ensure the response is properly formatted.");
            }

            return content.asText();

        } catch (Exception e) {
            throw new Exception("Failed to parse AI response: " + e.getMessage());
        }
    }

    @Override
    public String analyzeFile(String filename, String diffContent) {
        StringBuilder promptBuilder = new StringBuilder();
        final var future = new CompletableFuture();
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
        return promptBuilder.toString();
    }

}
