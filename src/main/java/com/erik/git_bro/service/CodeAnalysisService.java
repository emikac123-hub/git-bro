package com.erik.git_bro.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erik.git_bro.client.ChatGPTClient;
import com.erik.git_bro.client.GeminiClient;
import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.model.Category;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeAnalysisService {


    private final ChatGPTClient chatGPTClient;
    private final GeminiClient geminiClient;
    
    private final ReviewRepository reviewRepository;
    private final ReviewIterationService reviewIterationService;
    private final ParsingService parsingService;

    @Transactional
    public CompletableFuture<Object> analyzeDiff(AnalysisRequest request, String modelName) {

        // Find or create the iteration for this commit
        ReviewIteration iteration = reviewIterationService.findOrCreateIteration(request.pullRequestId(), request.sha());

        CompletableFuture<?> feedbackFuture;
        if ("chatgpt".equalsIgnoreCase(modelName)) {
            feedbackFuture = chatGPTClient.analyzeFileLineByLine(request.filename(), request.diffContent());
        } else if ("gemini".equalsIgnoreCase(modelName)) {
            feedbackFuture = geminiClient.analyzeFileLineByLine(request.filename(), request.diffContent());
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unsupported AI model: " + modelName));
        }

        return feedbackFuture
                .thenApplyAsync(feedback -> {
                    String feedbackCast = parsingService.cleanChunk((String) feedback);

                    Category issueCategory = getIssueCategory(feedbackCast);
                    BigDecimal severity = determineSeverity(issueCategory);

                    String fingerprint = createFingerprint(
                            request.pullRequestId(), request.filename(), request.diffContent(), issueCategory.name());

                    // Check if this exact feedback already exists for this PR
                    if (!reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(request.pullRequestId(), fingerprint)) {
                        Review review = Review.builder()
                                .pullRequestId(request.pullRequestId())
                                .fileName(request.filename())
                                .diffContent(request.diffContent())
                                .feedback(feedbackCast)
                                .category(issueCategory)
                                .feedbackFingerprint(fingerprint)
                                .derivedSeverityScore(severity)
                                .userId(request.author())
                                .prUrl(request.prUrl())
                                .createdAt(Instant.now())
                                .build();

                        iteration.addReview(review);
                        reviewRepository.save(review);
                        log.info("New unique feedback found and saved for file: {}", request.filename());
                    } else {
                        log.info("Duplicate feedback detected and skipped for file: {}", request.filename());
                    }
                    return feedback; // Return feedback for the controller
                });
    }

    private String createFingerprint(String pullRequestId, String filename, String diffContent, String issueCategory) {
        try {
            String sourceString = pullRequestId + ":" + filename + ":" + diffContent + ":" + issueCategory;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sourceString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not create SHA-256 fingerprint", e);
            // Fallback to a simple concatenation if SHA-256 is not available
            return pullRequestId + filename + diffContent;
        }
    }

    private Category getIssueCategory(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            return Category.NO_FEEDBACK;
        }
        feedback = feedback.toLowerCase();
        if (feedback.contains("null pointer") || feedback.contains("security")) {
            return Category.SECURITY;
        } else if (feedback.contains("performance") || feedback.contains("race condition")) {
            return Category.PERFORMANCE;
        } else if (feedback.contains("naming") || feedback.contains("style")) {
            return Category.STYLE;
        }
        return Category.GENERAL;
    }

    private BigDecimal determineSeverity(Category category) {
        return switch (category) {
            case SECURITY -> BigDecimal.valueOf(0.9);
            case PERFORMANCE -> BigDecimal.valueOf(0.7);
            case STYLE -> BigDecimal.valueOf(0.3);
            case GENERAL -> BigDecimal.valueOf(0.2);
            case NO_FEEDBACK -> BigDecimal.valueOf(0.1);
        };
    }
}