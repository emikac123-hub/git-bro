package com.erik.git_bro.ai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.Primary;

/**
 * Interface defining the contract for code analysis implementations.
 * <p>
 * Implementations of this interface provide methods to analyze code snippets,
 * parse AI responses, and analyze entire files asynchronously.
 * </p>
 * <p>
 * Marked with {@code @Primary} to indicate that, if multiple implementations exist,
 * this one should be the default injection candidate by Spring.
 * </p>
 */
@Primary
public interface CodeAnalyzer {

    /**
     * Analyzes the given list of code chunks.
     * 
     * @param chunkedInput a list of code segments to analyze
     * @return a string representing the analysis result or feedback
     * @throws IOException if an input/output error occurs during analysis
     * @throws Exception for other unexpected errors during analysis
     */
    String analyzeCode(List<String> chunkedInput) throws IOException, Exception;

    /**
     * Parses a raw AI response string into a meaningful result or message. Not used with ChatGPT, but may be useful later.
     * 
     * @param rawResponse the raw response string from the AI service
     * @return a string representing the parsed result or message
     * @throws Exception if the response cannot be parsed correctly
     */
    String parseAiResponse(String rawResponse) throws Exception;

    /**
     * Asynchronously analyzes the diff content of a file identified by its filename.
     * 
     * @param filename the name of the file to analyze
     * @param diffContent the diff content of the file to analyze
     * @return a {@link CompletableFuture} representing the pending result of the analysis,
     *         which will contain the feedback or analysis result
     */
    CompletableFuture<?> analyzeFile(String filename, String diffContent);
}
