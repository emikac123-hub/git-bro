package com.erik.git_bro.service.github;


import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;

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
    public void postInlineComment(
        String githubToken,
        String owner,
        String repo,
        int pullNumber,
        String filePath,
        int lineNumber,
        String commentBody,
        String sha) throws IOException {

    String url = String.format(
        "https://api.github.com/repos/%s/%s/pulls/%d/reviews",
        owner, repo, pullNumber);

    // Build request body
    Map<String, Object> json = Map.of(
        "path", filePath,
        "line", lineNumber,
        "side", "RIGHT", // always use "RIGHT" unless you're doing diff hunk parsing
        "body", commentBody,
        "commit_id", sha
    );

    String jsonBody = objectMapper.writeValueAsString(json);

    Request request = new Request.Builder()
        .url(url)
        .header("Authorization", "Bearer " + githubToken)
        .header("Accept", "application/vnd.github+json")
        .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
        .build();

    try (Response response = okClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            throw new IOException("GitHub comment failed: " + response.code() + " " + response.body().string());
        }
        log.info("Successfully posted PR inline comment on {} line {}", filePath, lineNumber);
    }
}

}

