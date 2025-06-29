package com.erik.git_bro.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.erik.git_bro.client.ChatGPTClient;
import com.erik.git_bro.client.GeminiClient;
import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.model.Category;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CodeAnalysisServiceTest {

    @Mock private ChatGPTClient chatGPTClient;
    @Mock private GeminiClient geminiClient;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewIterationService reviewIterationService;
    @Mock private ParsingService parsingService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private CodeAnalysisService codeAnalysisService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void analyzeDiff_chatGPT_model_success() throws Exception {
        AnalysisRequest request = new AnalysisRequest("123", "file.java", "diff", "author", "pr-url", "sha");
        ReviewIteration iteration = new ReviewIteration();

        String rawFeedback = "{\"issues\":[{\"file\":\"file.java\",\"line\":1,\"comment\":\"This is an issue.\"}]}";
        InlineReviewResponse response = new InlineReviewResponse(List.of(new Issue("file.java", 1, 0, "This is an issue.")), "merge");

        when(chatGPTClient.analyzeFileLineByLine(Mockito.anyString(), Mockito.anyString()))
            .thenReturn((CompletableFuture) CompletableFuture.completedFuture(rawFeedback));
        when(reviewIterationService.findOrCreateIteration(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(iteration);
        when(parsingService.cleanChunk(rawFeedback)).thenReturn(rawFeedback);
        when(objectMapper.readValue(rawFeedback, InlineReviewResponse.class)).thenReturn(response);
        when(parsingService.getIssueCategory("This is an issue.")).thenReturn(Category.GENERAL);
        when(reviewRepository.existsByPullRequestIdAndFeedbackFingerprint(any(), any())).thenReturn(false);
/**
 * return (CompletableFuture<String>) chatGPTClient.analyzeFileLineByLine(request.filename(),
                    request.diffContent());
 */
        CompletableFuture<InlineReviewResponse> future = codeAnalysisService.analyzeDiff(request, "chatgpt");
        InlineReviewResponse result = future.get();

        assertNotNull(result);
        assertEquals(1, result.getIssues().size());
        verify(reviewRepository).save(any(Review.class));
        verify(reviewIterationService).save(iteration);
    }

    @Test
    void analyzeDiff_invalid_model_throws() {
        AnalysisRequest request = new AnalysisRequest("id", "file.java", "diff", "author", "url", "sha");
        CompletableFuture<InlineReviewResponse> result = codeAnalysisService.analyzeDiff(request, "unknown");
        assertThrows(Exception.class, result::join);
    }

    @Test
    void createFingerprint_produces_non_null_hash() throws Exception {
        String result = codeAnalysisService.createFingerprint("1", "file.java", "diff", "GENERAL");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void determineSeverity_returns_correct_value() {
        assertEquals(BigDecimal.valueOf(0.9), codeAnalysisService.determineSeverity(Category.SECURITY));
        assertEquals(BigDecimal.valueOf(0.7), codeAnalysisService.determineSeverity(Category.PERFORMANCE));
        assertEquals(BigDecimal.valueOf(0.3), codeAnalysisService.determineSeverity(Category.STYLE));
        assertEquals(BigDecimal.valueOf(0.2), codeAnalysisService.determineSeverity(Category.GENERAL));
        assertEquals(BigDecimal.valueOf(0.1), codeAnalysisService.determineSeverity(Category.NO_FEEDBACK));
    }
} 
