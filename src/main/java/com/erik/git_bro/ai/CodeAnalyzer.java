package com.erik.git_bro.ai;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Primary;
@Primary
public interface CodeAnalyzer {
    static final String NO_ISSUES = "No issues detected.";
    String analyzeCode(List<String> chunkedInput) throws IOException, Exception;
    String parseAiResponse(String rawResponse) throws Exception;
}