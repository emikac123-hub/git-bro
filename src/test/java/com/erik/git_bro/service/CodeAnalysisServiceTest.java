package com.erik.git_bro.service;

import com.erik.git_bro.client.ChatGPTClient;
import com.erik.git_bro.client.GeminiClient;
import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.model.Category;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.client.ChatGPTClient;
import com.erik.git_bro.client.GeminiClient;
import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.model.Category;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewRepository;
import com.erik.git_bro.service.ParsingService;
import com.erik.git_bro.service.ReviewIterationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class CodeAnalysisServiceTest {

    @MockBean
    private ChatGPTClient chatGPTClient;

    @MockBean
    private GeminiClient geminiClient;

    @MockBean
    private ReviewRepository reviewRepository;

    @MockBean
    private ReviewIterationService reviewIterationService;

    @MockBean
    private ParsingService parsingService;

    @Autowired
    private CodeAnalysisService codeAnalysisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    public void testAnalyzeDiff() throws Exception {
        AnalysisRequest request = new AnalysisRequest("test.java", "diff --git a/test.java b/test.java\n--- a/test.java\n+++ b/test.java\n@@ -1,1 +1,1 @@\n-public class Test { }\n+public class Test { public void newMethod() { } }", "1", "test-sha", "http://test.url", "test-author");
        InlineReviewResponse response = new InlineReviewResponse(Collections.singletonList(new Issue("test.java", 1, 1, "Test comment")), "Test recommendation");

        when((CompletableFuture) geminiClient.analyzeFileLineByLine(any(), any())).thenReturn(CompletableFuture.completedFuture(objectMapper.writeValueAsString(response)));
        when(reviewIterationService.findOrCreateIteration(any(), any())).thenReturn(new ReviewIteration());
        when(parsingService.cleanChunk(any())).thenCallRealMethod();
        when(parsingService.getIssueCategory(any())).thenReturn(Category.GENERAL);

        codeAnalysisService.analyzeDiff(request, "gemini").get();
    }
}

