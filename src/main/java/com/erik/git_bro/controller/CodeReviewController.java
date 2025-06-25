package com.erik.git_bro.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.erik.git_bro.dto.GitDiff;
import com.erik.git_bro.dto.InlineReviewResponse;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.model.ErrorResponse;
import com.erik.git_bro.service.CodeAnalysisService;
import com.erik.git_bro.service.ParsingService;
import com.erik.git_bro.service.github.GitHubAppService;
import com.erik.git_bro.service.github.GitHubCommentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller that handles code review requests.
 * <p>
 * Provides an endpoint to receive a file containing code diffs and analyze them
 * asynchronously using
 * a code analysis service.
 * </p>
 */
@RestController
@RequestMapping("/api/review")
@Slf4j
@RequiredArgsConstructor
public class CodeReviewController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodeAnalysisService codeAnalysisService;
    private final ParsingService parsingService;
    private final GitHubAppService gitHubAppService;
    private final GitHubCommentService gitHubCommentService;

    /**
     * Analyzes the contents of a file asynchronously. Mainly used for posting a
     * Block Commet onto a GitHub PR Review.
     * <p>
     * Accepts a multipart form upload with a file parameter named "file". The file
     * contents are read
     * as a UTF-8 string representing a code diff. The diff is then passed to the
     * {@code CodeAnalysisService}
     * for asynchronous analysis.
     * </p>
     * <p>
     * The method returns a {@link CompletableFuture} that resolves to an HTTP
     * response:
     * <ul>
     * <li>{@code 200 OK} with the analysis feedback if successful</li>
     * <li>{@code 400 Bad Request} with an {@link ErrorResponse} if the analysis
     * failed due to invalid input</li>
     * <li>{@code 500 Internal Server Error} with an {@link ErrorResponse} for other
     * failures</li>
     * </ul>
     * </p>
     *
     * @param file the multipart uploaded file containing the code diff to analyze
     * @return a {@link CompletableFuture} that resolves to a {@link ResponseEntity}
     *         containing
     *         the analysis result or error information
     * @throws IOException if reading the uploaded file bytes fails
     */
    @PostMapping(value = "/analyze-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<?>> analyzeFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("owner") String owner,
            @RequestParam("repo") String repo,
            @RequestParam("pullNumber") int pullNumber) throws IOException {

        String diff = new String(file.getBytes(), StandardCharsets.UTF_8);

        return codeAnalysisService.analyzeFile(file.getOriginalFilename(), diff)
                .handle((feedback, throwable) -> {
                    return this.showResponse((String) feedback, throwable, "code analysis failed");
                });
    }

    @PostMapping(value = "/analyze-file-by-line", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<?>> postInlineComment(@RequestParam("file") MultipartFile file,
            @RequestParam("owner") String owner,
            @RequestParam("repo") String repo,
            @RequestParam("pullNumber") int pullNumber) throws IOException {
        String diff = new String(file.getBytes(), StandardCharsets.UTF_8);

        return this.codeAnalysisService.analyzeFileLineByLine(file.getName(), diff)
                .handle((feedback, throwable) -> {
                    if (throwable != null) {
                        return this.showResponse((String) feedback, throwable, "Failure to analyze code by line.");
                    }

                    try {
                        final String installationId = gitHubAppService.getInstallationId(owner, repo);
                        final String sha = gitHubAppService.getSha(owner, repo, pullNumber);
                        final String token = gitHubAppService.getInstallationToken(Long.parseLong(installationId));
                        final String cleanFeedback = ((String) feedback)
                                .replaceAll("(?s)```json\\s*", "")
                                .replaceAll("(?s)```", "")
                                .trim();
                        final List<GitDiff> diffsFromPr = this.gitHubAppService.getDiffs(owner, repo, pullNumber);
                        InlineReviewResponse inlineResponse = objectMapper.readValue(cleanFeedback,
                                InlineReviewResponse.class);
                        for (Issue issue : inlineResponse.getIssues()) {
                            String issueFile = issue.getFile();
                            int line = issue.getLine();
                            String comment = issue.getComment();

                            // Find the matching GitDiff
                            Optional<GitDiff> matchingDiff = diffsFromPr.stream()
                                    .filter(d -> d.getFilename().equals(issueFile))
                                    .findFirst();

                            if (matchingDiff.isPresent()) {
                                Set<Integer> validLines = this.parsingService
                                        .extractCommentableLines(matchingDiff.get().getPatch());

                                if (validLines.contains(line)) {
                                    gitHubCommentService.postBlockComments(
                                            token,
                                            owner,
                                            repo,
                                            pullNumber,
                                            issueFile,
                                            line,
                                            comment,
                                            sha);
                                } else {
                                    log.warn("Skipping comment: line {} in {} is not part of diff.", line, file);
                                }
                            } else {
                                log.warn("No diff found for file: {}", file);
                            }
                        }
                        StringBuilder markdownSummary = new StringBuilder();
                        markdownSummary.append("### 🤖 AI Review Summary\n");
                        markdownSummary.append("Posted ").append(inlineResponse.getIssues().size())
                                .append(" inline comments.\n\n");

                        for (Issue issue : inlineResponse.getIssues()) {
                            markdownSummary
                                    .append("- **File**: `").append(issue.getFile()).append("`\n")
                                    .append("  - **Line**: ").append(issue.getLine()).append("\n")
                                    .append("  - **Comment**: ").append(issue.getComment().replaceAll("\n", " ").trim())
                                    .append("\n\n");
                        }

                        return ResponseEntity.ok()
                                .body(markdownSummary.toString().trim());

                    } catch (Exception e) {
                        log.error("Failed to parse or post inline comments", e);
                        return ResponseEntity.status(500).body("Failed to post inline comments: " + e.getMessage());
                    }
                });
    }

    private ResponseEntity<?> showResponse(final String feedback, final Throwable throwable, String logMessage) {
        if (throwable == null) {
            return ResponseEntity.ok().body(feedback);
        }

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        log.error(logMessage, cause);

        var error = ErrorResponse.builder()
                .message(cause.getMessage())
                .details(cause.getCause() != null ? cause.getCause().getLocalizedMessage() : "")
                .build();

        if (cause instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(error);
        }

        return ResponseEntity.status(500).body(error);
    }
}
