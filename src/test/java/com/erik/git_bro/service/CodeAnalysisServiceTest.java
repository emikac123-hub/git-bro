package com.erik.git_bro.service;

import java.util.Collections;
import java.util.List;
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
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class CodeAnalysisServiceTest {

        private ChatGPTClient chatGPTClient;
        private GeminiClient geminiClient;
        private ReviewRepository reviewRepository;
        private ParsingService parsingService;
        private ReviewIterationService reviewIterationService;
        private ObjectMapper objectMapper;
        private CodeAnalysisService codeAnalysisService;

        @BeforeEach
        void setUp() {
                chatGPTClient = mock(ChatGPTClient.class);
                geminiClient = mock(GeminiClient.class);
                reviewRepository = mock(ReviewRepository.class);
                parsingService = mock(ParsingService.class);
                reviewIterationService = mock(ReviewIterationService.class);
                objectMapper = mock(ObjectMapper.class);

                codeAnalysisService = new CodeAnalysisService(
                                chatGPTClient,
                                geminiClient,
                                reviewRepository,
                                reviewIterationService,
                                parsingService,
                                objectMapper);
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
                                "test-author");

                String rawFeedback = "```json\n{\"issues\":[{\"file\":\"TestFile.java\",\"line\":10,\"comment\":\"Security issue found.\"}]}\n```";
                String cleanedFeedback = "{\"issues\":[{\"file\":\"TestFile.java\",\"line\":10,\"comment\":\"Security issue found.\"}]}";

                ReviewIteration iteration = new ReviewIteration();

                Issue mockIssue = new Issue();
                mockIssue.setFile("TestFile.java");
                mockIssue.setLine(10);
                mockIssue.setComment("Security issue found.");

                InlineReviewResponse inlineReviewResponse = new InlineReviewResponse();
                inlineReviewResponse.setIssues(List.of(mockIssue));

                when(parsingService.cleanChunk(rawFeedback)).thenReturn(cleanedFeedback);
                when(reviewIterationService.findOrCreateIteration(request.pullRequestId(), request.sha()))
                                .thenReturn(iteration);
                when(chatGPTClient.analyzeFileLineByLine(anyString(), anyString()))
                                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(rawFeedback));
                when(parsingService.cleanChunk(rawFeedback)).thenReturn(cleanedFeedback);
                when(objectMapper.readValue(cleanedFeedback, InlineReviewResponse.class))
                                .thenReturn(inlineReviewResponse);
                when(reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(eq(request.pullRequestId()),
                                anyString()))
                                .thenReturn(false);

                // When
                CompletableFuture<?> future = codeAnalysisService.analyzeDiff(request, "chatgpt");
                InlineReviewResponse result = (InlineReviewResponse) future.get();

                ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
                verify(reviewRepository).save(reviewCaptor.capture());
                Review savedReview = reviewCaptor.getValue();

                assertEquals(request.filename(), savedReview.getFileName());
                assertEquals(request.diffContent(), savedReview.getDiffContent());
                assertEquals(request.pullRequestId(), savedReview.getPullRequestId());
                assertEquals(request.author(), savedReview.getUserId());
                assertNotNull(savedReview.getFeedback());
                assertNotNull(savedReview.getFeedbackFingerprint());
                assertEquals(iteration, savedReview.getReviewIteration());
                assertNotNull(result);
                assertEquals(1, result.getIssues().size());
                assertEquals("Security issue found.", result.getIssues().get(0).getComment());
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
                                "test-author");

                String rawFeedback = "{\"issues\": [], \"recommendation\": \"Looks fine.\"}";
                String cleanedFeedback = rawFeedback;

                ReviewIteration iteration = new ReviewIteration();

                InlineReviewResponse inlineReviewResponse = new InlineReviewResponse();
                inlineReviewResponse.setIssues(Collections.emptyList());
                inlineReviewResponse.setRecommendation("Looks fine.");

                when(reviewIterationService.findOrCreateIteration(request.pullRequestId(), request.sha()))
                                .thenReturn(iteration);
                when(chatGPTClient.analyzeFileLineByLine(anyString(), anyString()))
                                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(rawFeedback));
                when(parsingService.cleanChunk(rawFeedback)).thenReturn(cleanedFeedback);
                when(objectMapper.readValue(cleanedFeedback, InlineReviewResponse.class))
                                .thenReturn(inlineReviewResponse);
                when(reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(eq(request.pullRequestId()),
                                anyString()))
                                .thenReturn(true); // Simulate duplicate feedback found

                // When
                CompletableFuture<?> future = codeAnalysisService.analyzeDiff(request, "chatgpt");
                Object result = future.get();

                // Then
                assertNotNull(result, "Result should not be null");
                assertTrue(result instanceof InlineReviewResponse, "Result should be InlineReviewResponse instance");

                InlineReviewResponse response = (InlineReviewResponse) result;
                assertEquals(0, response.getIssues().size());
                assertEquals("Looks fine.", response.getRecommendation());
                verify(reviewRepository, never()).save(any(Review.class));
                assertTrue(iteration.getReviews().isEmpty());
        }

}
