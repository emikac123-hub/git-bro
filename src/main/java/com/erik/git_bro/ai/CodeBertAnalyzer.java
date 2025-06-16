package com.erik.git_bro.ai;

import com.erik.git_bro.client.CodeBertClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component("codeBertAnalyzer")
public class CodeBertAnalyzer implements CodeAnalyzer {

    private final CodeBertClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeBertAnalyzer(CodeBertClient client) {
        this.client = client;
    }

    @Override
    public String analyzeCode(List<String> chunkedInput) throws Exception {
        return client.analyzeCode(chunkedInput);
    }

    @Override
    public String parseAiResponse(String rawResponse) {
        try {
            final List<List<Double>> embeddings = objectMapper.readValue(rawResponse,
                    new TypeReference<List<List<Double>>>() {});
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

    @Override
    public CompletableFuture<String> analyzeFile(String filename, String diffContent) {
    
        return CompletableFuture.completedFuture(null);
    }
}
