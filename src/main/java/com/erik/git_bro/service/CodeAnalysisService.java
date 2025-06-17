package com.erik.git_bro.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.ReviewRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for analyzing code diffs using an AI-based code analyzer,
 * persisting the analysis results as {@link Review} entities, and optionally
 * writing feedback to a file.
 * <p>
 * This service asynchronously processes code diffs by sending them to an AI
 * analyzer,
 * then saves the feedback along with relevant metadata (file path, diff
 * content, timestamp)
 * into the database. It also provides functionality to persist feedback logs to
 * a
 * configured file path.
 * </p>
 * <p>
 * The {@link #analyzeFile(String, String)} method returns a
 * {@link CompletableFuture}
 * that completes with the AI feedback once analysis and persistence are done.
 * </p>
 */
@Service
@Slf4j
public class CodeAnalysisService {

    /**
     * File system path to which feedback logs are written asynchronously.
     * Configured via property 'app.feedback.file-path'.
     */
    @Value("${app.feedback.file-path}")
    private String feedbackFilePath;

    /**
     * AI-based code analyzer component used to generate feedback for code diffs.
     */
    private final CodeAnalyzer analyzer;

    /**
     * Repository for persisting {@link Review} entities containing code analysis
     * results.
     */
    private final ReviewRepository reviewRepository;

    /**
     * Constructs a new {@code CodeAnalysisService} with injected dependencies.
     *
     * @param analyzer         the AI code analyzer to use for generating feedback
     * @param parsingService   service to extract metadata from code diffs
     * @param reviewRepository repository to persist review entities
     */
    public CodeAnalysisService(@Qualifier("codeAnalyzer") CodeAnalyzer analyzer,
            final ParsingService parsingService,
            final ReviewRepository reviewRepository) {
        this.analyzer = analyzer;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Analyzes a given code diff asynchronously by sending it to the AI code
     * analyzer.
     * Once the analysis completes, a {@link Review} entity is created and saved
     * containing
     * the analysis feedback, diff content, extracted file path, and creation
     * timestamp.
     *
     * @param filename    the name of the file being analyzed (used for context)
     * @param diffContent the unified diff text representing code changes
     * @return a {@link CompletableFuture} that completes with the AI-generated
     *         feedback string
     */
    public CompletableFuture<?> analyzeFile(String filename, String diffContent) {
        return analyzer.analyzeFile(filename, diffContent)
                .thenApply(feedback -> {
                    final var feedbackCast = (String) feedback;
                    final var review = Review.builder()
                            .createdAt(Instant.now())
                            .fileName(filename)
                            .prUrl(null)
                            .pullRequestId(null)
                            .issueFlag(null)
                            .diffContent(diffContent)
                            .feedback((String) feedbackCast)
                            .severityScore((BigDecimal) this.determineSeverity(feedbackCast))
                            .build();
                    reviewRepository.save(review);
                    return feedback;
                });
    }

    /**
     * Writes feedback for a specific pull request asynchronously to a configured
     * file.
     * The feedback is appended with a timestamp and PR identifier.
     * This method is non-blocking and exceptions during file writing are logged but
     * not propagated.
     *
     * @param pullRequestId the unique identifier of the pull request
     * @param feedback      the feedback string to write to the file
     * @return a {@link CompletableFuture} that completes when the write operation
     *         finishes
     */
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
                // File writing is non-critical, so exceptions are swallowed.
            }
        });
    }

    /**
     * A severity score to measure the issues found in the PR.
     * Eventually, this will be displayed on a dashbaord on the UI.
     * @param feedback - AI Generated Feedback
     * @return
     */
    private BigDecimal determineSeverity(String feedback) {
        feedback = feedback.toLowerCase();

        if (feedback.contains("null pointer") || feedback.contains("security")) {
            return BigDecimal.valueOf(0.9);
        } else if (feedback.contains("performance") || feedback.contains("race condition")) {
            return BigDecimal.valueOf(0.7);
        } else if (feedback.contains("naming") || feedback.contains("style")) {
            return BigDecimal.valueOf(0.3);
        }

        return BigDecimal.valueOf(0.2); // default medium severity
    }
}
