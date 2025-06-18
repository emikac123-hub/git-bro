package com.erik.git_bro.controller;

import com.erik.git_bro.model.ErrorResponse;
import com.erik.git_bro.service.CodeAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CodeReviewControllerTest {

    private CodeReviewController controller;
    private CodeAnalysisService codeAnalysisService;

    @BeforeEach
    void setUp() {
        codeAnalysisService = Mockito.mock(CodeAnalysisService.class);
        controller = new CodeReviewController(codeAnalysisService);
    }

    @Test
    void analyzeFromFile_successfulAnalysis_returnsOk() throws Exception {
        // Given
        String diffContent = "diff --git a/Test.java b/Test.java\n";
        String expectedFeedback = "Looks good!";

        MockMultipartFile file = new MockMultipartFile(
                "file", "Test.java.diff", "text/plain", diffContent.getBytes(StandardCharsets.UTF_8));

        when(codeAnalysisService.analyzeFile(anyString(), anyString()))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(expectedFeedback));

        // When
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.analyzeFromFile(file);
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedFeedback, response.getBody());
    }

    @Test
    void analyzeFromFile_illegalArgumentException_returnsBadRequest() throws Exception {
        // Given
        String diffContent = "diff --git a/Test.java b/Test.java\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "Test.java.diff", "text/plain", diffContent.getBytes(StandardCharsets.UTF_8));

        CompletableFuture<?> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalArgumentException("Invalid diff"));

        when(codeAnalysisService.analyzeFile(anyString(), anyString()))
                .thenReturn((CompletableFuture) failedFuture);

        // When
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.analyzeFromFile(file);
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ErrorResponse);

        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Invalid diff", error.getMessage());
    }

    @Test
    void analyzeFromFile_unexpectedException_returnsInternalServerError() throws Exception {
        // Given
        String diffContent = "diff --git a/Test.java b/Test.java\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "Test.java.diff", "text/plain", diffContent.getBytes(StandardCharsets.UTF_8));

        CompletableFuture<?> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Something went wrong"));

        when(codeAnalysisService.analyzeFile(anyString(), anyString()))
                .thenReturn((CompletableFuture) failedFuture);

        // When
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.analyzeFromFile(file);
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ErrorResponse);

        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Something went wrong", error.getMessage());
    }
}
