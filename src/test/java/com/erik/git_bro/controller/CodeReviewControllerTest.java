package com.erik.git_bro.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.dto.ErrorResponse;
import com.erik.git_bro.dto.GitDiff;
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.model.Category;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.service.CodeAnalysisService;
import com.erik.git_bro.service.ParsingService;
import com.erik.git_bro.service.github.GitHubAppService;
import com.erik.git_bro.service.github.GitHubAppTokenService;
import com.erik.git_bro.service.github.GitHubCommentService;
import com.fasterxml.jackson.databind.ObjectMapper;

class CodeReviewControllerTest {

    private CodeReviewController controller;
    private CodeAnalysisService codeAnalysisService;
    private GitHubAppService gitHubAppService;
    private GitHubCommentService gitHubCommentService;
    private GitHubAppTokenService gitHubAppTokenService;
    private ParsingService parsingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        codeAnalysisService = Mockito.mock(CodeAnalysisService.class);
        gitHubAppService = Mockito.mock(GitHubAppService.class);
        gitHubCommentService = Mockito.mock(GitHubCommentService.class);
        gitHubAppTokenService = Mockito.mock(GitHubAppTokenService.class);
        parsingService = Mockito.mock(ParsingService.class);
        controller = new CodeReviewController(codeAnalysisService, parsingService, gitHubAppService, gitHubCommentService, gitHubAppTokenService);
    }

    @Test
    void postInlineComment_successfulAnalysis_returnsOk() throws Exception {
        // Given
        String diffContent = "diff --git a/Test.java b/Test.java\n" +
                             "--- a/Test.java\n" +
                             "+++ b/Test.java\n" +
                             "@@ -1,1 +1,1 @@\n" +
                             "+public class Test {}";
        MockMultipartFile file = new MockMultipartFile(
                "file", "Test.java", "text/plain", diffContent.getBytes(StandardCharsets.UTF_8));

        Review review = Review.builder()
        .line(10)
        .category(Category.GENERAL)
        .createdAt(Instant.now())
        .feedback("Feedback")
        .feedbackFingerprint("102103")
        .build();
        String owner = "erikmikac";
        String repo = "git-bro";
        int pullNumber = 1;
        String prUrl = "http://example.com/pr/1";
        String prAuthor = "test-author";
        String sha = "test-sha";
        String installationId = "12345";
        String token = "test-token";

        Issue issue = new Issue();
        issue.setFile("Test.java");
        issue.setLine(1);
        issue.setComment("This is a test comment.");
        InlineReviewResponse inlineReviewResponse = new InlineReviewResponse();
        inlineReviewResponse.setIssues(List.of(issue));

        String feedbackJson = objectMapper.writeValueAsString(inlineReviewResponse);

        GitDiff gitDiff = new GitDiff();
        gitDiff.setFilename("Test.java");
        gitDiff.setPatch("@@ -1,1 +1,1 @@\n+public class Test {}");


        when(gitHubAppService.getSha(owner, repo, pullNumber)).thenReturn(sha);
        when(codeAnalysisService.analyzeDiff(any(AnalysisRequest.class), eq("chatgpt")))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(review));
        when(gitHubAppTokenService.getInstallationId(owner, repo)).thenReturn(installationId);
        when(gitHubAppTokenService.getInstallationToken(Long.parseLong(installationId))).thenReturn(token);
        when(gitHubAppService.getDiffs(owner, repo, pullNumber)).thenReturn(List.of(gitDiff));
        when(parsingService.extractCommentableLines(anyString())).thenReturn(Set.of(1));

        // When
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.postInlineComment(file, owner, repo, pullNumber, prUrl, prAuthor, "chatgpt");
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("AI Review Summary"));
        verify(gitHubCommentService).postReviewCommentBatch(eq(token), eq(owner), eq(repo), eq(pullNumber), anyList());
    }

    @Test
    void postInlineComment_analysisFailsWithIllegalArgument_returnsBadRequest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.diff", "text/plain", "diff".getBytes());
        String owner = "erikmikac";
        String repo = "git-bro";
        int pullNumber = 1;
        String prUrl = "http://example.com/pr/1";
        String prAuthor = "test-author";
        String sha = "test-sha";

        when(gitHubAppService.getSha(owner, repo, pullNumber)).thenReturn(sha);

        CompletableFuture<?> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalArgumentException("Invalid diff"));
        when((CompletableFuture) codeAnalysisService.analyzeDiff(any(AnalysisRequest.class), anyString())).thenReturn(failedFuture);

        // When
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.postInlineComment(file, owner, repo, pullNumber, prUrl, prAuthor, "chatgpt");
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Invalid diff", error.getMessage());
    }

    @Test
    void postInlineComment_analysisFailsWithRuntimeException_returnsInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.diff", "text/plain", "diff".getBytes());
        String owner = "erikmikac";
        String repo = "git-bro";
        int pullNumber = 1;
        String prUrl = "http://example.com/pr/1";
        String prAuthor = "test-author";
        String sha = "test-sha";

        when(gitHubAppService.getSha(owner, repo, pullNumber)).thenReturn(sha);

        CompletableFuture<?> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Something went wrong"));
        when((CompletableFuture) codeAnalysisService.analyzeDiff(any(AnalysisRequest.class), anyString())).thenReturn(failedFuture);

        // When
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.postInlineComment(file, owner, repo, pullNumber, prUrl, prAuthor, "chatgpt");
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Something went wrong", error.getMessage());
    }

    @Test
    void postInlineComment_getShaFails_returnsInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.diff", "text/plain", "diff".getBytes());
        String owner = "erikmikac";
        String repo = "git-bro";
        int pullNumber = 1;
        String prUrl = "http://example.com/pr/1";
        String prAuthor = "test-author";

        when(gitHubAppService.getSha(owner, repo, pullNumber)).thenThrow(new IOException("Failed to get SHA"));

        // When
        CompletableFuture<ResponseEntity<?>> responseFuture = controller.postInlineComment(file, owner, repo, pullNumber, prUrl, prAuthor, "chatgpt");
        ResponseEntity<?> response = responseFuture.get();

        // Then
        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to prepare analysis: Failed to get SHA", response.getBody());
    }
}
