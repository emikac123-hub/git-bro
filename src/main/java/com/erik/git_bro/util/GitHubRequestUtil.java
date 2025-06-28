package com.erik.git_bro.util;

import okhttp3.Request;

public class GitHubRequestUtil {

        public static Request.Builder withGitHubHeaders(Request.Builder builder, String githubToken) {
        return builder
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json");
    }

}
