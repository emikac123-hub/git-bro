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

    /**
     * Analyzes the provided code difference using the specified AI model.
     * This method orchestrates the AI feedback retrieval, parsing, and persistence.
     *
     * @param request   The analysis request containing filename, diff content, and
     *                  other details.
     * @param modelName The name of the AI model to use (e.g., "chatgpt", "gemini").
     * @return A CompletableFuture that will hold the InlineReviewResponse from the
     *         AI model.
     * @throws IllegalArgumentException if the AI model is unsupported.
     */
    @Transactional
    public CompletableFuture<InlineReviewResponse> analyzeDiff(AnalysisRequest request, String modelName) {
        CompletableFuture<String> feedbackFuture;
        try {
            feedbackFuture = getAIFeedbackFuture(request, modelName);
        } catch (IllegalArgumentException e) {
            CompletableFuture<InlineReviewResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }

        ReviewIteration iteration = reviewIterationService.findOrCreateIteration(request.pullRequestId(),
                request.sha());

        return feedbackFuture.thenApplyAsync(rawFeedback -> {
            try {
                return processAiFeedback(rawFeedback, request, iteration, modelName);
            } catch (JsonProcessingException e) {
                log.error("Error parsing AI feedback: {}", rawFeedback, e);
                throw new RuntimeException("Failed to parse AI feedback", e);
            }
        });
    }

    /**
     * Retrieves the AI feedback as a CompletableFuture based on the specified
     * model.
     *
     * @param request   The analysis request.
     * @param modelName The name of the AI model.
     * @return A CompletableFuture containing the raw feedback string from the AI.
     * @throws IllegalArgumentException if the AI model is unsupported.
     */
    private CompletableFuture<String> getAIFeedbackFuture(AnalysisRequest request, String modelName) {
        if ("chatgpt".equalsIgnoreCase(modelName)) {
            return (CompletableFuture<String>) chatGPTClient.analyzeFileLineByLine(request.filename(),
                    request.diffContent());
        } else if ("gemini".equalsIgnoreCase(modelName)) {
            return (CompletableFuture<String>) geminiClient.analyzeFileLineByLine(request.filename(),
                    request.diffContent());
        } else {
            throw new IllegalArgumentException("Unsupported AI model: " + modelName);
        }
    }

    /**
     * Processes the raw AI feedback, parses it, and persists the review details.
     *
     * @param rawFeedback The raw feedback string received from the AI.
     * @param request     The original analysis request.
     * @param iteration   The current review iteration.
     * @param modelName   The name of the AI model used.
     * @return The parsed InlineReviewResponse.
     * @throws JsonProcessingException if there's an error parsing the JSON
     *                                 feedback.
     */
    private InlineReviewResponse processAiFeedback(String rawFeedback, AnalysisRequest request,
            ReviewIteration iteration, String modelName) throws JsonProcessingException {
        InlineReviewResponse inlineReviewResponse = parseFeedback(rawFeedback);

        BigDecimal maxSeverity = inlineReviewResponse.getIssues().stream()
                .map(issue -> processIssue(issue, request, iteration))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        iteration.setDerivedSeverityScore(maxSeverity);
        iteration.setAiModel(modelName);
        reviewIterationService.save(iteration);

        return inlineReviewResponse;
    }

    /**
     * Parses the raw AI feedback string into an InlineReviewResponse object.
     *
     * @param rawFeedback The raw feedback string from the AI.
     * @return The parsed InlineReviewResponse.
     * @throws JsonProcessingException if there's an error parsing the JSON
     *                                 feedback.
     * @throws RuntimeException        if the parsed response is null.
     */
    private InlineReviewResponse parseFeedback(String rawFeedback) throws JsonProcessingException {
        String cleanFeedback = parsingService.cleanChunk(rawFeedback);
        InlineReviewResponse response = objectMapper.readValue(cleanFeedback, InlineReviewResponse.class);
        if (response == null) {
            log.error("Parsed inlineReviewResponse is null for feedback: {}", cleanFeedback);
            throw new RuntimeException("Failed to parse AI feedback: inlineReviewResponse is null");
        }
        return response;
    }

    /**
     * Processes an individual AI issue, determines its severity, creates a
     * fingerprint,
     * and saves it as a new review if it's not a duplicate.
     *
     * @param aiIssue   The AI-generated issue.
     * @param request   The original analysis request.
     * @param iteration The current review iteration.
     * @return The severity of the processed issue.
     */
    private BigDecimal processIssue(Issue aiIssue, AnalysisRequest request, ReviewIteration iteration) {
        Category issueCategory = parsingService.getIssueCategory(aiIssue.getComment());
        BigDecimal severity = determineSeverity(issueCategory);
        String fingerprint = createFingerprint(request.pullRequestId(), aiIssue.getFile(), aiIssue.getComment(),
                issueCategory.name());

        if (!reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(request.pullRequestId(), fingerprint)) {
            Review review = createReview(aiIssue, request, issueCategory, severity, fingerprint);
            iteration.addReview(review);
            reviewRepository.save(review);
            log.info("New unique feedback found and saved for file: {}", aiIssue.getFile());
        } else {
            log.info("Duplicate feedback detected and skipped for file: {}", aiIssue.getFile());
        }
        return severity;
    }

    /**
     * Creates a new Review object from the provided issue and request details.
     *
     * @param aiIssue     The AI-generated issue.
     * @param request     The original analysis request.
     * @param category    The category of the issue.
     * @param severity    The determined severity of the issue.
     * @param fingerprint The unique fingerprint of the feedback.
     * @return A new Review object.
     */
    private Review createReview(Issue aiIssue, AnalysisRequest request, Category category, BigDecimal severity,
            String fingerprint) {
        return Review.builder()
                .pullRequestId(request.pullRequestId())
                .fileName(aiIssue.getFile())
                .diffContent(request.diffContent())
                .feedback(aiIssue.getComment())
                .category(category)
                .feedbackFingerprint(fingerprint)
                .severityScore(severity)
                .userId(request.author())
                .prUrl(request.prUrl())
                .createdAt(Instant.now())
                .line(aiIssue.getLine())
                .build();
    }

    /**
     * Creates a SHA-256 fingerprint for a given feedback entry to identify
     * duplicates.
     *
     * @param pullRequestId The ID of the pull request.
     * @param filename      The name of the file.
     * @param diffContent   The content of the diff.
     * @param issueCategory The category of the issue.
     * @return A SHA-256 hash as a String, or a concatenated string if hashing
     *         fails.
     */
    public String createFingerprint(String pullRequestId, String filename, String diffContent, String issueCategory) {
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

    /**
     * Determines the severity score for a given issue category.
     *
     * @param category The category of the issue.
     * @return A BigDecimal representing the severity score.
     */
    public BigDecimal determineSeverity(Category category) {
        return switch (category) {
            case SECURITY -> BigDecimal.valueOf(0.9);
            case PERFORMANCE -> BigDecimal.valueOf(0.7);
            case STYLE -> BigDecimal.valueOf(0.3);
            case GENERAL -> BigDecimal.valueOf(0.2);
            case NO_FEEDBACK -> BigDecimal.valueOf(0.1);
        };
    }
}