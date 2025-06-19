package com.erik.git_bro.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.AiModelRepository;
import com.erik.git_bro.repository.ReviewRepository;

class CodeAnalysisServiceTest {

    private AiModelRepository aiModelRepository;
    private CodeAnalyzer codeAnalyzer;
    private ReviewRepository reviewRepository;
    private ParsingService parsingService;

    private CodeAnalysisService codeAnalysisService;

    @BeforeEach
    void setUp() {
        codeAnalyzer = mock(CodeAnalyzer.class);
        reviewRepository = mock(ReviewRepository.class);
        parsingService = mock(ParsingService.class);
        aiModelRepository = mock(AiModelRepository.class);
        codeAnalysisService = new CodeAnalysisService(codeAnalyzer, parsingService, reviewRepository,
                aiModelRepository);
    }

    @Test
    void analyzeFile_shouldAnalyzeAndPersistReview() throws Exception {
        // Given
        String filename = "TestFile.java";
        String diffContent = "diff --git a/TestFile.java b/TestFile.java";
        String feedback = "Possible null pointer exception detected in line 42.";
        when(parsingService.cleanChunk(anyString())).thenReturn(feedback);
        
        
        when(codeAnalyzer.analyzeFile(anyString(), anyString()))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(feedback));

        // When
        CompletableFuture<?> future = codeAnalysisService.analyzeFile(filename, diffContent);
        Object result = future.join(); // block to complete

        // Then: verify feedback returned
        assertEquals(feedback, result);

        // Then: verify Review was persisted
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository, times(1)).save(reviewCaptor.capture());

        Review savedReview = reviewCaptor.getValue();
        assertNotNull(savedReview);
        assertEquals(filename, savedReview.getFileName());
        assertEquals(diffContent, savedReview.getDiffContent());
        assertEquals(feedback, savedReview.getFeedback());
        assertTrue(savedReview.getSeverityScore().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(savedReview.getCreatedAt());
    }

}
