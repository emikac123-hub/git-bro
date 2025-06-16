package com.erik.git_bro.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;


@Service

@Slf4j
public class CodeAnalysisService {
    private final ParsingService parsingService;
    @Value("${app.feedback.file-path}")
    private String feedbackFilePath;
    private final ReviewRepository reviewRepository;
    private final CodeAnalyzer analyzer;

    public CodeAnalysisService(@Qualifier("codeAnalyzer") CodeAnalyzer analyzer,
            ReviewRepository reviewRepository,
            final ParsingService parsingService) {
        this.analyzer = analyzer;
        this.reviewRepository = reviewRepository;
        this.parsingService = parsingService;
    }

    public CompletableFuture<?> analyzeFile(String filename, String diffContent) {
         return CompletableFuture.completedFuture(analyzer.analyzeFile(filename, diffContent));
    }

    // TODO - May keep this around for troubleshooting.
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
