package com.erik.git_bro.service.github;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.util.API;
import com.erik.git_bro.util.GitHubRequestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@AllArgsConstructor
@Slf4j
public class GitHubCommentService {

    private final OkHttpClient okClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void postBlockComments(
            String githubToken,
            String owner,
            String repo,
            int pullNumber,
            String filePath,
            int lineNumber,
            String commentBody,
            String sha) throws IOException {

        String url = API.GIT_HUB_COMMENTS(owner, repo, pullNumber);

        // Build request body
        Map<String, Object> json = Map.of(
                "body", commentBody,
                "commit_id", sha,
                "path", filePath,
                "line", lineNumber,
                "side", "RIGHT");

        String jsonBody = objectMapper.writeValueAsString(json);

        Request request = GitHubRequestUtil.withGitHubHeaders(
                new Request.Builder().url(url), githubToken)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = okClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GitHub comment failed: " + response.code() + " " + response.body().string());
            }
            log.info("Successfully posted PR inline comment on {} line {}", filePath, lineNumber);

        }
    }

    public void postReviewCommentBatch(
            String githubToken,
            String owner,
            String repo,
            int pullNumber,
            List<Issue> issues) throws IOException {

        // Convert each Issue to a GitHub review comment map
        List<Map<String, Object>> reviewComments = issues.stream()
                .map(issue -> {
                    Map<String, Object> comment = new HashMap<>();
                    comment.put("path", issue.getFile());
                    comment.put("line", issue.getLine());
                    comment.put("side", "RIGHT");
                    comment.put("body", issue.getComment());
                    return comment;
                })
                .collect(Collectors.toList());

        Map<String, Object> reviewBody = Map.of(
                "body", "AI Review: Suggested inline improvements.",
                "event", "COMMENT",
                "comments", reviewComments);

        String reviewUrl = API.GIT_HUB_REVIEWS(owner, repo, pullNumber);
        
        Request request = GitHubRequestUtil.withGitHubHeaders(
                new Request.Builder().url(reviewUrl), githubToken)
                .post(RequestBody.create(objectMapper.writeValueAsString(reviewBody), MediaType.parse("application/json")))
                .build();

        try (Response response = okClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GitHub review failed: " + response.code() + " " + response.body().string());
            }
            log.info("✅ Successfully posted AI review with {} comments.", issues.size());
        }
    }

}