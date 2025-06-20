package com.erik.git_bro.ai;

import com.erik.git_bro.client.CodeBertClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code CodeBertAnalyzer} is an implementation of the {@link CodeAnalyzer}
 * interface
 * that uses the CodeBERT model client to analyze code snippets.
 * <p>
 * It processes code input as chunks and parses the AI response to detect
 * potential code issues
 * such as possible null pointer exceptions and style violations based on
 * embedding analysis.
 * </p>
 * <p>
 * This component uses {@link CodeBertClient} to communicate with the CodeBERT
 * AI model,
 * and Jackson's {@link ObjectMapper} to parse JSON responses.
 * </p>
 */
@Component("codeBertAnalyzer")
public class CodeBertAnalyzer implements CodeAnalyzer {

    private final CodeBertClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructs a {@code CodeBertAnalyzer} with the specified CodeBERT client.
     *
     * @param client the CodeBERT client used to perform code analysis
     */
    public CodeBertAnalyzer(CodeBertClient client) {
        this.client = client;
    }

    /**
     * Parses the raw JSON response returned from CodeBERT and analyzes the
     * embeddings
     * to detect possible code issues.
     * <p>
     * This method interprets the response as a list of vectors (embeddings) and
     * computes
     * average values to heuristically flag potential problems such as:
     * <ul>
     * <li>Possible null pointer exceptions (if max mean embedding > 0.1)</li>
     * <li>Style violations (if second embedding's mean > 0.05)</li>
     * </ul>
     * If no issues are detected, it returns "No issues detected."
     * </p>
     *
     * @param rawResponse the raw JSON string response from CodeBERT
     * @return a human-readable string summarizing detected issues or a failure
     *         message
     */
    @Override
    public String parseAiResponse(String rawResponse) {
        try {
            final List<List<Double>> embeddings = objectMapper.readValue(rawResponse,
                    new TypeReference<List<List<Double>>>() {
                    });
            List<String> issues = new ArrayList<>();

            List<Double> vectorMeans = embeddings.stream()
                    .map(vector -> vector.stream()
                            .mapToDouble(Double::doubleValue)
                            .average().orElse(0.0))
                    .toList();

            double maxMean = vectorMeans.stream()
                    .mapToDouble(Double::doubleValue)
                    .max().orElse(0.0);

            if (maxMean > 0.1) {
                issues.add("Possible null pointer exception.");
            }
            if (embeddings.size() > 1 && vectorMeans.get(1) > 0.05) {
                issues.add("Style violation detected.");
            }

            return issues.isEmpty() ? "No issues detected." : String.join("; ", issues);
        } catch (Exception e) {
            return "Failed to parse CodeBERT response.";
        }
    }

    /**
     * Asynchronously analyzes a file based on its filename and diff content.
     * <p>
     * This implementation returns a completed future with {@code null},
     * meaning this method is not currently supported.
     * </p>
     *
     * @param filename    the name of the file to analyze
     * @param diffContent the diff content of the file
     * @return a completed {@link CompletableFuture} with {@code null}
     */
    @Override
    public CompletableFuture<String> analyzeFile(String filename, String diffContent) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> analyzeFileLineByLine(String filename, String diffContent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<String> sendJavaDocPrompt(String methodText) {
        // TODO Auto-generated method stub
        return CompletableFuture.completedFuture(null);
    }
}
