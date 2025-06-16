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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CodeAnalysisService {
    @Value("${app.feedback.file-path}")
    private String feedbackFilePath;
    private final CodeAnalyzer analyzer;

    public CodeAnalysisService(@Qualifier("codeAnalyzer") CodeAnalyzer analyzer,
            final ParsingService parsingService) {
        this.analyzer = analyzer;

    }

    public CompletableFuture<?> analyzeFile(String filename, String diffContent) {

        return analyzer.analyzeFile(filename, diffContent);
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
