package com.erik.git_bro.dto;

public record AnalysisRequest(
    String filename,
    String diffContent,
    String pullRequestId,
    String sha,
    String prUrl,
    String author
) {}