package com.erik.git_bro.controller;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.erik.git_bro.model.ErrorResponse;
import com.erik.git_bro.service.CodeAnalysisService;
import com.erik.git_bro.service.github.GitHubAppService;
import com.erik.git_bro.service.github.GitHubCommentService;

class CodeReviewControllerTest {

    private CodeReviewController controller;
    private CodeAnalysisService codeAnalysisService;
    private GitHubAppService gitHubAppService;
    private GitHubCommentService gitHubCommentService;

    @BeforeEach
    void setUp() {
        codeAnalysisService = Mockito.mock(CodeAnalysisService.class);
        gitHubAppService = Mockito.mock(GitHubAppService.class);
        gitHubCommentService = Mockito.mock(GitHubCommentService.class);
        controller = new CodeReviewController(codeAnalysisService, gitHubAppService, gitHubCommentService);
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
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.analyzeFromFile(file, "erikmikac","git-bro",1);
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
      CompletableFuture<ResponseEntity<?>> responseFuture = controller.analyzeFromFile(file, "erikmikac","git-bro",1);
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
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.analyzeFromFile(file, "erikmikac","git-bro",1);
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ErrorResponse);

        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Something went wrong", error.getMessage());
    }
}
