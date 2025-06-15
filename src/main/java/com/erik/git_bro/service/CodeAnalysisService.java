package com.erik.git_bro.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.config.AiProviderProperties;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.ReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service

@Slf4j
public class CodeAnalysisService {

    private final AiProviderProperties aiProviderProperties;
    @Value("${app.feedback.file-path}")
    private String feedbackFilePath;
    private final ReviewRepository reviewRepository;
    private final CodeAnalyzer analyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int CHUNK_SIZE = 200;
    private static final int freeTierAPILimit = 20;

    public CodeAnalysisService(@Qualifier("codeAnalyzer") CodeAnalyzer analyzer,
            AiProviderProperties aiProviderProperties,
            ReviewRepository reviewRepository) {
        this.analyzer = analyzer;
        this.reviewRepository = reviewRepository;
        this.aiProviderProperties = aiProviderProperties;
    }

    @Async("virtualThreadExecutor")
    public CompletableFuture<Object> analyzeDiff(final String pullRequestId,
            final String rawDiffContent) {
        try {
            // First, peel away everything excpet what was added. 
            final var diffContent = this.extractAddedLinesOnly(rawDiffContent);
            log.info("Extracted Content: {}", diffContent);
            if (pullRequestId == null || diffContent == null) {
                throw new IllegalArgumentException("Input parameters cannot be null");
            }

            List<String> chunks = this.chunkItUp(diffContent);

            log.info("Split diffContent for PR {} into {} chunks", pullRequestId, chunks.size());

            List<String> feedbacks = chunks.stream()
                    .map(chunk -> {

                        try {
                            final String feedback = analyzer.analyzeCode(chunk);
                            return analyzer.parseAiResponse(feedback);
                        } catch (final IOException e) {
                            log.error("Failed to process chunk for PR {}: {}.", pullRequestId, e.getMessage());
                            return "Error analyzing chunk";
                        } catch (Exception e) {

                            log.error("An Unknown Exception occured for PR {}: {}.", pullRequestId, e.getMessage());
                            return "Uknown Exception analyzing chunk";
                        }

                    })
                    .filter(fb -> !fb.equals("Nothing significant issues found") && !fb.equals(CodeAnalyzer.NO_ISSUES))
                    .distinct()
                    .collect(Collectors.toList());
            // Aggregate feedback
            String feedback = feedbacks.isEmpty() ? "No significant issues detected" : String.join("; ", feedbacks);
            writeFeedbackToFile(pullRequestId, feedback);
            log.info("The Actual Feedback");
            log.info(feedback);
            Review review = new Review();
            review.setReviewId(UUID.randomUUID().toString());
            review.setPullRequestId(pullRequestId);
            review.setFilePath(this.extractFilePathFromDiff(diffContent));
            review.setDiffContent(diffContent);
            review.setFeedback(feedback);
            review.setCreatedAt(Instant.now().toString());

            return CompletableFuture.completedFuture(reviewRepository.save(review));
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<String> chunkItUp(final String diffContent) throws JsonProcessingException {
        List<String> chunks = new ArrayList<>();
        if (this.aiProviderProperties.getAiProvider().equals("chatgpt")) {
            chunks.add(diffContent);
            return chunks;
        }
        for (int i = 0; i < diffContent.length(); i += CHUNK_SIZE) {
            String chunk = diffContent.substring(i, Math.min(i + CHUNK_SIZE, diffContent.length()));
            chunk = chunk.length() > CHUNK_SIZE ? chunk.substring(0, CHUNK_SIZE) : chunk;
            String escapedChunk = objectMapper.writeValueAsString(chunk);
            log.debug("json payload: {}", escapedChunk);
            chunks.add(cleanChunk(escapedChunk));
        }
        if (chunks.size() > freeTierAPILimit) {
            int overFlow = chunks.size() - freeTierAPILimit;
            while (overFlow > 0) {
                chunks.removeLast();
                --overFlow;
            }
        }
        return chunks;
    }

    private String extractFilePathFromDiff(String diffContent) {
        Pattern pattern = Pattern.compile("^\\+\\+\\+ b/(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(diffContent);
        return matcher.find() ? matcher.group(1) : "unknown";
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

    // TODO - Eventually, chunk by file. Right now, I can't do that because I am on
    // the free version.
    private List<String> chunkByFile(String diffContent) {
        List<String> fileChunks = new ArrayList<>();

        String[] parts = diffContent.split("(?=^diff --git )", -1);
        for (String part : parts) {
            if (part.strip().isEmpty()) {
                continue; // skip empty entries
            }
            fileChunks.add(part.strip());
        }

        return fileChunks;
    }

    private String cleanChunk(String chunk) {
        return chunk.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", ""); // Remove illegal control characters
    }

    public String extractAddedLinesOnly(String diff) {
        return Arrays.stream(diff.split("\n"))
                .filter(line -> line.startsWith("+") && !line.startsWith("+++"))
                .map(line -> line.substring(1)) // strip leading '+'
                .collect(Collectors.joining("\n"));
    }

}
