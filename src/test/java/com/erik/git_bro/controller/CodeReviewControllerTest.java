package com.erik.git_bro.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.erik.git_bro.dto.GitDiff;
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.service.CodeAnalysisService;
import com.erik.git_bro.service.ParsingService;
import com.erik.git_bro.service.github.GitHubAppService;
import com.erik.git_bro.service.github.GitHubAppTokenService;
import com.erik.git_bro.service.github.GitHubCommentService;

class CodeReviewControllerTest {

    private CodeAnalysisService codeAnalysisService;
    private ParsingService parsingService;
    private GitHubAppService gitHubAppService;
    private GitHubAppTokenService gitHubAppTokenService;
    private GitHubCommentService gitHubCommentService;

    private CodeReviewController controller;

    @BeforeEach
    void setUp() {
        codeAnalysisService = mock(CodeAnalysisService.class);
        parsingService = mock(ParsingService.class);
        gitHubAppService = mock(GitHubAppService.class);
        gitHubAppTokenService = mock(GitHubAppTokenService.class);
        gitHubCommentService = mock(GitHubCommentService.class);

        controller = new CodeReviewController(
                codeAnalysisService,
                parsingService,
                gitHubAppService,
                gitHubCommentService,
                gitHubAppTokenService);
    }

    @Test
    void postInlineComment_successfulReview_returnsMarkdownSummary() throws Exception {
        // Given
        MultipartFile mockFile = new MockMultipartFile("file", "TestFile.java", "text/plain",
                "diff --git a/TestFile.java b/TestFile.java".getBytes(StandardCharsets.UTF_8));
        String owner = "testOwner";
        String repo = "testRepo";
        int pullNumber = 123;
        String prUrl = "http://example.com/pr/123";
        String prAuthor = "author";
        String modelName = "chatgpt";
        String sha = "abc123";

        Issue issue = new Issue("TestFile.java", 10, 5, "Security issue");
        InlineReviewResponse inlineReviewResponse = new InlineReviewResponse(List.of(issue), "Looks secure now.");

        GitDiff gitDiff = GitDiff.builder()
                .filename("TestFile.java")
                .patch("@@ -1,1 +1,1 @@")
                .status("modified")
                .deletions("1")
                .additions("1")
                .changes("2")
                .sha("abc123")
                .build();

        when(gitHubAppService.getSha(owner, repo, pullNumber)).thenReturn(sha);
        when(codeAnalysisService.analyzeDiff(any(), eq(modelName)))
                .thenReturn(CompletableFuture.completedFuture(inlineReviewResponse));
        when(gitHubAppTokenService.getInstallationId(owner, repo)).thenReturn("42");
        when(gitHubAppTokenService.getInstallationToken(42L)).thenReturn("token-123");
        when(gitHubAppService.getDiffs(owner, repo, pullNumber)).thenReturn(List.of(gitDiff));
        when(parsingService.extractCommentableLines(any())).thenReturn(Set.of(10));
        when(parsingService.calculatePositionInDiffHunk(any(), eq(10))).thenReturn(5);

        // When
        ResponseEntity<?> response = controller
                .postInlineComment(mockFile, owner, repo, pullNumber, prUrl, prAuthor, modelName).get();

        // Then
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        String body = response.getBody().toString();
        assertTrue(body.contains("AI Review Summary"));
        assertTrue(body.contains("Security issue"));
        // verify(gitHubCommentService).postBlockComments(any(), eq(owner), eq(repo), eq(pullNumber),
        //         eq("TestFile.java"), eq(5),  eq("Security issue"), eq(sha));
      //  verify(gitHubCommentService).postReviewCommentBatch(any(), eq(owner), eq(repo), eq(pullNumber), any());
    }

    @Test
    void postInlineComment_duplicateFeedback_returnsSkipMessage() throws Exception {
        MultipartFile mockFile = new MockMultipartFile("file", "TestFile.java", "text/plain",
                "diff --git a/TestFile.java b/TestFile.java".getBytes(StandardCharsets.UTF_8));
        when(codeAnalysisService.analyzeDiff(any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        ResponseEntity<?> response = controller.postInlineComment(
                mockFile, "owner", "repo", 1, "http://example.com/pr/1", "author", "chatgpt").get();

        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertEquals("No new feedback generated or duplicate feedback skipped.", response.getBody());
    }

    @Test
    void postInlineComment_errorFromAnalysis_returnsErrorResponse() throws Exception {
        MultipartFile mockFile = new MockMultipartFile("file", "TestFile.java", "text/plain",
                "diff --git a/TestFile.java b/TestFile.java".getBytes(StandardCharsets.UTF_8));
        when(codeAnalysisService.analyzeDiff(any(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalArgumentException("Bad input")));

        ResponseEntity<?> response = controller.postInlineComment(
                mockFile, "owner", "repo", 1, "http://example.com/pr/1", "author", "chatgpt").get();

        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Bad input"));
    }

    @Test
    void postInlineComment_unexpectedException_returns500() throws Exception {
        MultipartFile mockFile = new MockMultipartFile("file", "TestFile.java", "text/plain",
                "some content".getBytes(StandardCharsets.UTF_8));
        when(gitHubAppService.getSha(any(), any(), anyInt())).thenThrow(new RuntimeException("SHA fail"));

        ResponseEntity<?> response = controller.postInlineComment(
                mockFile, "owner", "repo", 1, "url", "author", "model").get();

        assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
        assertTrue(response.getBody().toString().contains("SHA fail"));
    }
}
