package com.erik.git_bro.ai;

import java.io.IOException;

import org.springframework.context.annotation.Primary;
@Primary
public interface CodeAnalyzer {
    static final String NO_ISSUES = "No issues detected.";
    String analyzeCode(String input) throws IOException, Exception;
    String parseAiResponse(String rawResponse);
}