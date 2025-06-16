package com.erik.git_bro.ai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.Primary;
@Primary
public interface CodeAnalyzer {
    String analyzeCode(List<String> chunkedInput) throws IOException, Exception;
    String parseAiResponse(String rawResponse) throws Exception;
    CompletableFuture<?>  analyzeFile(String filename, String diffContent);
}