package com.erik.git_bro.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiUrlProvider {
    @Value("${gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${github.api-base-url}")
    private String githubApiBaseUrl;

    public String getInstallationTokenUrl(long installationId) {
        return String.format("%s/app/installations/%s/access_tokens", githubApiBaseUrl, installationId);
    }

    public String getCommentsUrl(String owner, String repo, int pullNumber) {
        return String.format("%s/repos/%s/%s/pulls/%s/comments", githubApiBaseUrl, owner, repo, pullNumber);
    }

    public String getInstallationIdUrl(String owner, String repo) {
        return String.format("%s/repos/%s/%s/installation",githubApiBaseUrl, owner, repo);
    }

    public String getGeminiUrl() {
        return geminiBaseUrl;
    }

}
