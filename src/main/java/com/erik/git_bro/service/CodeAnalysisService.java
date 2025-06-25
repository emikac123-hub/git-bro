package com.erik.git_bro.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.AiModelRepository;
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

    private final ParsingService parsingService;

    private final AiModelRepository aiModelRepository;

    /**
     * Constructs a new {@code CodeAnalysisService} with injected dependencies.
     *
     * @param analyzer          the AI code analyzer to use for generating feedback
     * @param parsingService    service to extract metadata from code diffs
     * @param reviewRepository  repository to persist review entities
     * @param aiModelRepository repository to persist review entities
     */
    public CodeAnalysisService(@Qualifier("codeAnalyzer") CodeAnalyzer analyzer,
            final ParsingService parsingService,
            final ReviewRepository reviewRepository,
            final AiModelRepository aiModeRepository) {
        this.analyzer = analyzer;
        this.reviewRepository = reviewRepository;
        this.parsingService = parsingService;
        this.aiModelRepository = aiModeRepository;

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
                    // Sanitize feedback before inserting into DB.
                    final var feedbackCast = (String) this.parsingService.cleanChunk((String) feedback);
                    final var review = Review.builder()
                            .createdAt(Instant.now())
                            .fileName(filename)
                            .prUrl(null)
                            .pullRequestId(null)
                            .issueFlag(null)
                            .diffContent(diffContent)
                            // .aiModel(review.setAiModel(aiModelRepository.findById(aiModelId).orElseThrow(()
                            // -> log.err));)
                            .feedback((String) feedbackCast)
                            .severityScore((BigDecimal) this.determineSeverity(feedbackCast))
                            .build();

                    reviewRepository.save(review);
                    log.info("database insertion complete");
                    return feedback;
                });
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
    public CompletableFuture<?> analyzeFileLineByLine(String filename, String diffContent) {
        return analyzer.analyzeFileLineByLine(filename, diffContent)
                .thenApply(feedback -> {
                    // Sanitize feedback before inserting into DB.
                    final var feedbackCast = (String) this.parsingService.cleanChunk((String) feedback);
                    final var review = Review.builder()
                            .createdAt(Instant.now())
                            .fileName(filename)
                            .prUrl(null)
                            .pullRequestId(null)
                            .issueFlag(null)
                            .diffContent(diffContent)
                            // .aiModel(review.setAiModel(aiModelRepository.findById(aiModelId).orElseThrow(()
                            // -> log.err));)
                            .feedback((String) feedbackCast)
                            .severityScore((BigDecimal) this.determineSeverity(feedbackCast))
                            .build();
                    reviewRepository.save(review);
                    log.info("database insertion complete");
                    return feedback;
                });
    }

    /**
     * A severity score to measure the issues found in the PR.
     * Eventually, this will be displayed on a dashbaord on the UI.
     * 
     * @param feedback - AI Generated Feedback
     * @return
     */
    private BigDecimal determineSeverity(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            log.warn("The feedback is null, this could indicate a problem!");
            return BigDecimal.valueOf(0.1); // lowest severity if feedback is blank
        }

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
