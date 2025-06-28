package com.erik.git_bro.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erik.git_bro.client.ChatGPTClient;
import com.erik.git_bro.client.GeminiClient;
import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewRepository;

class CodeAnalysisServiceTest {
    private ChatGPTClient client;
    private ReviewRepository reviewRepository;
    private ParsingService parsingService;
    private ReviewIterationService reviewIterationService;
    private CodeAnalysisService codeAnalysisService;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        parsingService = mock(ParsingService.class);
        reviewIterationService = mock(ReviewIterationService.class);
        client = mock(ChatGPTClient.class);
        codeAnalysisService = new CodeAnalysisService(client, mock(GeminiClient.class), reviewRepository, reviewIterationService, parsingService);
    }

    @Test
    void analyzeDiff_newFeedback_savesReview() throws Exception {
        // Given
        AnalysisRequest request = new AnalysisRequest(
                "TestFile.java",
                "diff --git a/TestFile.java b/TestFile.java",
                "pr-1",
                "sha-123",
                "http://example.com/pr/1",
                "test-author"
        );
        String rawFeedback = "```json\n{\"issues\":[{\"file\":\"TestFile.java\",\"line\":10,\"comment\":\"Security issue found.\"}]}\n```";
        String cleanedFeedback = "{\"issues\":[{\"file\":\"TestFile.java\",\"line\":10,\"comment\":\"Security issue found.\"}]}";

        ReviewIteration iteration = new ReviewIteration();

        when(reviewIterationService.findOrCreateIteration(request.pullRequestId(), request.sha())).thenReturn(iteration);
        when(client.analyzeFileLineByLine(anyString(), anyString()))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(rawFeedback));
        when(parsingService.cleanChunk(rawFeedback)).thenReturn(cleanedFeedback);
        when(reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(eq(request.pullRequestId()), anyString()))
                .thenReturn(false);

        // When
        CompletableFuture<Object> future = codeAnalysisService.analyzeDiff(request, "chatgpt");
        Object result = future.get();

        // Then
        assertEquals(rawFeedback, result, "The raw feedback should be returned to the controller.");

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());

        Review savedReview = reviewCaptor.getValue();
        assertNotNull(savedReview);
        assertEquals(request.filename(), savedReview.getFileName());
        assertEquals(request.diffContent(), savedReview.getDiffContent());
        assertEquals(cleanedFeedback, savedReview.getFeedback());
        assertEquals(request.pullRequestId(), savedReview.getPullRequestId());
        assertEquals(request.author(), savedReview.getUserId());
        assertEquals(0, new BigDecimal("0.9").compareTo(savedReview.getDerivedSeverityScore())); // Security category
        assertNotNull(savedReview.getFeedbackFingerprint());
        assertTrue(iteration.getReviews().contains(savedReview));
    }

    @Test
    void analyzeDiff_duplicateFeedback_skipsSave() throws Exception {
        // Given
        AnalysisRequest request = new AnalysisRequest(
                "TestFile.java",
                "diff --git a/TestFile.java b/TestFile.java",
                "pr-1",
                "sha-123",
                "http://example.com/pr/1",
                "test-author"
        );
        String rawFeedback = "This is a duplicate comment.";
        String cleanedFeedback = "This is a duplicate comment.";

        ReviewIteration iteration = new ReviewIteration();

        when(reviewIterationService.findOrCreateIteration(request.pullRequestId(), request.sha())).thenReturn(iteration);
        when(client.analyzeFileLineByLine(anyString(), anyString()))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(rawFeedback));
        when(parsingService.cleanChunk(rawFeedback)).thenReturn(cleanedFeedback);
        when(reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(eq(request.pullRequestId()), anyString()))
                .thenReturn(true); // Simulate that this feedback already exists

        // When
        CompletableFuture<Object> future = codeAnalysisService.analyzeDiff(request, "chatgpt");
        Object result = future.get();

        // Then
        assertEquals(rawFeedback, result);
        verify(reviewRepository, never()).save(any(Review.class));
        assertTrue(iteration.getReviews().isEmpty());
    }
}
