package com.erik.git_bro.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.erik.git_bro.client.CodeBertClient;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.ReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeAnalysisService {
    @Value("${app.feedback.file-path}")
    private String feedbackFilePath;
    private final CodeBertClient codeBertClient;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async("virtualThreadExecutor")
    public CompletableFuture<Object> analyzeDiff(final String pullRequestId, final String filePath,
            final String diffContent) {
        try {
            if (pullRequestId == null || filePath == null || diffContent == null) {
                throw new IllegalArgumentException("Input parameters cannot be null!");
            }

            String feedback = codeBertClient.analyzeCode(diffContent);
            writeFeedbackToFile(pullRequestId, feedback);
            log.info("The Actual Feedback");
            log.info(feedback);
            feedback = parseAiResponse(feedback);

            Review review = new Review();
            review.setReviewId(UUID.randomUUID().toString());
            review.setPullRequestId(pullRequestId);
            review.setFilePath(filePath);
            review.setDiffContent(diffContent);
            review.setFeedback(feedback);
            review.setCreatedAt(Instant.now().toString());

            return CompletableFuture.completedFuture(reviewRepository.save(review));
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private String parseAiResponse(final String rawResponse) {
        try {
            final List<List<Double>> embeddings = objectMapper.readValue(rawResponse,
                    new TypeReference<List<List<Double>>>() {
                    });

            log.info("Parsed {} embedding vectors, each with {} dimensions",
            embeddings.size(), embeddings.isEmpty() ? 0 : embeddings.get(0).size());
            // Compute mean of each vector for simple analysis
            List<Double> vectorMeans = embeddings.stream()
                    .map(vector -> vector.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0))
                    .collect(Collectors.toList());
                    
            // Simple heuristic: high mean in any vector suggests an issue

            double maxMean = vectorMeans.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0.0);
            List<String> issues = new ArrayList<>();


            if (vectorMeans.get(0) > 0.1) {
                issues.add("Possible null pointer exception");
            }
            if (embeddings.size() > 1 && vectorMeans.get(1) > 0.05) {
                issues.add("Style violation detected.");
            }
            return issues.isEmpty() ? "No Issues detects" : String.join("; ", issues);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse CodeBERT response: {}", e.getMessage());
            return "Error analyzing code: Unable to parse AI response";
        } catch (Exception e) {
            log.error("Unexpected error parsing CodeBERT response: {}", e.getMessage());
            return "Error analyzing code: Unexpected issue.";
        }

    }

    private CompletableFuture<Void> writeFeedbackToFile(String pullRequestId, String feedback) {
        return CompletableFuture.runAsync(() -> {
            try {
                String entry = String.format("[%s] PR: %s%n%s%n%n",
                        Instant.now().toString(), pullRequestId, feedback);
                Files.write(Path.of(feedbackFilePath), entry.getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                log.info("Wrote feedback for PR {} to file: {}", pullRequestId, feedbackFilePath);
            } catch (IOException e) {
                log.error("Failed to write feedback for PR {} to file {}: {}",
                        pullRequestId, feedbackFilePath, e.getMessage());
                // Don't throw; file writing is non-critical
            }
        });
    }

}
