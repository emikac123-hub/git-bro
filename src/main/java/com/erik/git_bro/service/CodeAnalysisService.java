package com.erik.git_bro.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service

@Slf4j
public class CodeAnalysisService {

    private final ParsingService parsingService;
    @Value("${app.feedback.file-path}")
    private String feedbackFilePath;
    private final ReviewRepository reviewRepository;
    private final CodeAnalyzer analyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeAnalysisService(@Qualifier("codeAnalyzer") CodeAnalyzer analyzer,
            ReviewRepository reviewRepository,
            final ParsingService parsingService) {
        this.analyzer = analyzer;
        this.reviewRepository = reviewRepository;
        this.parsingService = parsingService;
    }

    @Async("virtualThreadExecutor")
    public CompletableFuture<Object> analyzeDiff(final String pullRequestId,
            final String rawDiffContent) {
        try {
            final var parseJSON = this.parsingService.extractInput(rawDiffContent);
            final var filePath = this.parsingService.extractFilePathFromDiff(rawDiffContent);

            log.info("raw content: {}", rawDiffContent);
            if (pullRequestId == null || rawDiffContent == null) {
                throw new IllegalArgumentException("Input parameters cannot be null");
            }
            final var diffContent = this.parsingService.filterAndExtractDiffLines(parseJSON);
            log.info("diffcontent: {}", diffContent);
            List<String> chunks = this.parsingService.splitDiffIntoChunks(diffContent, 1000);
            log.info("chunks: {}", chunks);
            String feedback = this.analyzer.analyzeCode(chunks);
            
            writeFeedbackToFile(pullRequestId, feedback);
            log.info("The Actual Feedback");
            log.info(feedback);
            Review review = new Review();
            review.setReviewId(UUID.randomUUID().toString());
            review.setPullRequestId(pullRequestId);
            review.setFilePath(filePath);
            review.setDiffContent(diffContent);
            review.setFeedback(this.analyzer.parseAiResponse(feedback));
            review.setCreatedAt(Instant.now().toString());

            return CompletableFuture.completedFuture(reviewRepository.save(review));
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
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
