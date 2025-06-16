package com.erik.git_bro.ai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.Primary;
@Primary
public interface CodeAnalyzer {
    static final String NO_ISSUES = "No issues detected.";
    String parseAiResponse(String rawResponse) throws Exception;
    String analyzeFile(String filename, String diffContent);
}