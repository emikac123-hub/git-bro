package com.erik.git_bro.controller;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.service.CodeAnalysisService;
import com.erik.git_bro.service.ParsingService;
import com.erik.git_bro.service.github.GitHubAppService;
import com.erik.git_bro.service.github.GitHubAppTokenService;
import com.erik.git_bro.service.github.GitHubCommentService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CodeReviewController.class)
public class CodeReviewControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CodeAnalysisService codeAnalysisService() {
            return Mockito.mock(CodeAnalysisService.class);
        }

        @Bean
        public ParsingService parsingService() {
            return Mockito.mock(ParsingService.class);
        }

        @Bean
        public GitHubAppService gitHubAppService() {
            return Mockito.mock(GitHubAppService.class);
        }

        @Bean
        public GitHubCommentService gitHubCommentService() {
            return Mockito.mock(GitHubCommentService.class);
        }

        @Bean
        public GitHubAppTokenService gitHubAppTokenService() {
            return Mockito.mock(GitHubAppTokenService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CodeAnalysisService codeAnalysisService;

    @Autowired
    private GitHubAppService gitHubAppService;

    @Test
    @WithMockUser
    public void testPostInlineComment() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.diff", "text/plain",
                "diff --git a/test.java b/test.java\n--- a/test.java\n+++ b/test.java\n@@ -1,1 +1,1 @@\n-public class Test { }\n+public class Test { public void newMethod() { } }"
                        .getBytes());

        when(gitHubAppService.getSha(any(), any(), anyInt())).thenReturn("test-sha");
        when(codeAnalysisService.analyzeDiff(any(AnalysisRequest.class), any()))
                .thenReturn(CompletableFuture.completedFuture(new InlineReviewResponse(
                        Collections.singletonList(new Issue("test.java", 1, 1, "Test comment")),
                        "Test recommendation")));

        mockMvc.perform(multipart("/api/review/analyze-file-by-line")
                .file(file)
                .param("owner", "test-owner")
                .param("repo", "test-repo")
                .param("pullNumber", "1")
                .param("prUrl", "http://test.url")
                .param("prAuthor", "test-author")
                .param("modelName", "test-model")
                .with(csrf()) // 👈 this needs to go inside the builder, not outside
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

    }
}
