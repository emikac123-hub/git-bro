package com.erik.git_bro.util;

public class API {

    public static String GIT_HUB_COMMENTS(final String owner, final String repo, final int pullNumber) {
        return String.format(
                "https://api.github.com/repos/%s/%s/pulls/%s/comments",
                owner, repo, pullNumber);
    }

    public static String GIT_HUB_INSTALLATION_ID(final String installationId) {
        return String.format("https://api.github.com/app/installations/%s/access_tokens", installationId);

    }

    public static String GIT_HUB_REVIEWS(final String owner, final String repo, final int pullNumber) {
        return String.format(
                "https://api.github.com/repos/%s/%s/pulls/%d/reviews", owner, repo, pullNumber);
    }

    public static String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

}
