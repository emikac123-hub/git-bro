package com.erik.git_bro.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import com.erik.git_bro.client.ChatGPTClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the {@link CodeAnalyzer} interface that utilizes
 * the OpenAI ChatGPT API via {@link ChatGPTClient} to analyze code changes.
 * <p>
 * This component sends code diffs or chunks of code to the ChatGPT service
 * for AI-driven analysis and parses the JSON response to extract meaningful feedback.
 * </p>
 * <p>
 * Supports asynchronous analysis of entire files through the
 * {@link #analyzeFile(String, String)} method, returning a {@link CompletableFuture}.
 * </p>
 */
@Component("chatGptAnalyzer")
public class ChatGptAnalyzer implements CodeAnalyzer {

    private final ChatGPTClient client;
   
    /**
     * Constructs a new {@code ChatGptAnalyzer} with the given ChatGPT client.
     *
     * @param client the {@link ChatGPTClient} used to communicate with the ChatGPT API
     */
    public ChatGptAnalyzer(ChatGPTClient client) {
        this.client = client;
    }

    /**
     * Analyzes a list of code chunks by forwarding them to the ChatGPT client.
     * This method performs a synchronous operation and may throw exceptions
     * if the underlying client encounters errors.
     *
     * @param chunkedInput a list of strings representing chunks of code or diffs
     * @return the raw string feedback from the AI model
     * @throws Exception if the ChatGPT client fails to process the input
     */
    @Override
    public String analyzeCode(List<String> chunkedInput) throws Exception {
        return client.analyzeCode(chunkedInput);
    }

    /**
     * Parses the JSON response returned by the AI model to extract the
     * content of the feedback message.
     * <p>
     * Expects the response JSON structure to contain a 'choices' array,
     * where the first element contains a 'message' object with a 'content' field.
     * </p>
     *
     * @param aiJsonResponse the raw JSON string response from the AI model
     * @return the extracted feedback content as a string
     * @throws Exception if parsing fails or expected fields are missing
     */
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
                        "The AI response did not contain a 'message.content' field! Please ensure the response is properly formatted.");
            }

            return content.asText();

        } catch (Exception e) {
            throw new Exception("Failed to parse AI response: " + e.getMessage());
        }
    }

    /**
     * Asynchronously analyzes the code diff of a file by delegating to the ChatGPT client.
     * Returns a {@link CompletableFuture} that completes with the AI-generated feedback.
     *
     * @param filename    the name of the file being analyzed
     * @param diffContent the diff content to analyze
     * @return a {@link CompletableFuture} that resolves with the AI feedback string
     */
    @Override
    public CompletableFuture<?> analyzeFile(String filename, String diffContent) {
        return this.client.analyzeFile(filename, diffContent);
    }
}
