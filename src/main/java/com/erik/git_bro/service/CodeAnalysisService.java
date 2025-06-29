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
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.model.Category;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;

    @Transactional
    public CompletableFuture<InlineReviewResponse> analyzeDiff(AnalysisRequest request, String modelName) {

        // Find or create the iteration for this commit
        ReviewIteration iteration = reviewIterationService.findOrCreateIteration(request.pullRequestId(),
                request.sha());

        CompletableFuture<String> feedbackFuture;
        if ("chatgpt".equalsIgnoreCase(modelName)) {
            feedbackFuture = (CompletableFuture<String>) chatGPTClient.analyzeFileLineByLine(request.filename(),
                    request.diffContent());
        } else if ("gemini".equalsIgnoreCase(modelName)) {
            feedbackFuture = (CompletableFuture<String>) geminiClient.analyzeFileLineByLine(request.filename(),
                    request.diffContent());
        } else {
            CompletableFuture<InlineReviewResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Unsupported AI model: " + modelName));
            return future;
        }

        return feedbackFuture
                .thenApplyAsync(rawFeedback -> {
                    try {
                        String cleanFeedback = parsingService.cleanChunk(rawFeedback);
                        InlineReviewResponse inlineReviewResponse = objectMapper.readValue(cleanFeedback,
                                InlineReviewResponse.class);

                        for (Issue aiIssue : inlineReviewResponse.getIssues()) {
                            Category issueCategory = getIssueCategory(aiIssue.getComment());
                            BigDecimal severity = determineSeverity(issueCategory);
                            Integer lineNumber = aiIssue.getLine(); // AI should provide line number

                            String fingerprint = createFingerprint(
                                    request.pullRequestId(), aiIssue.getFile(), aiIssue.getComment(),
                                    issueCategory.name());
               
                            // Check if this exact feedback already exists for this PR
                            if (!reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(request.pullRequestId(),
                                    fingerprint)) {
                                Review review = Review.builder()
                                        .pullRequestId(request.pullRequestId())
                                        .fileName(aiIssue.getFile())
                                        .diffContent(request.diffContent())
                                        .feedback(aiIssue.getComment())
                                        .category(issueCategory)
                                        .feedbackFingerprint(fingerprint)
                                        .derivedSeverityScore(severity)
                                        .userId(request.author())
                                        .prUrl(request.prUrl())
                                        .createdAt(Instant.now())
                                        .line(lineNumber)
                                        .build();

                                iteration.addReview(review);
                                reviewRepository.save(review);
                                log.info("New unique feedback found and saved for file: {}", aiIssue.getFile());
                            } else {
                                log.info("Duplicate feedback detected and skipped for file: {}", aiIssue.getFile());
                            }
                        }
                        return inlineReviewResponse; // Return the parsed response
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing AI feedback: {}", rawFeedback, e);
                        throw new RuntimeException("Failed to parse AI feedback", e);
                    }
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