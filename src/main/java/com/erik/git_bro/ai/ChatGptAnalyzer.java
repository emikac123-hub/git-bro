package com.erik.git_bro.ai;

import org.springframework.stereotype.Component;

import com.erik.git_bro.client.ChatGPTClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("chatGptAnalyzer")
public class ChatGptAnalyzer implements CodeAnalyzer {

    private final ChatGPTClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatGptAnalyzer(ChatGPTClient client) {
        this.client = client;
    }

    @Override
    public String analyzeCode(String input) throws Exception {
        return client.analyzeCode(input);
    }

    @Override
    public String parseAiResponse(String rawResponse) {
        try {
            JsonNode node = objectMapper.readTree(rawResponse);
            return node.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            return "Failed to parse ChatGPT response.";
        }
    }
}
