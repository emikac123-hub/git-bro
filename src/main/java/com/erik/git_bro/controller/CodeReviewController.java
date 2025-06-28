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

import com.erik.git_bro.dto.AnalysisRequest;
import com.erik.git_bro.dto.ErrorResponse;
import com.erik.git_bro.dto.GitDiff;
import com.erik.git_bro.dto.Issue;
import com.erik.git_bro.service.CodeAnalysisService;
import com.erik.git_bro.service.ParsingService;
import com.erik.git_bro.service.github.GitHubAppService;
import com.erik.git_bro.service.github.GitHubAppTokenService;
import com.erik.git_bro.service.github.GitHubCommentService;

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
    private final CodeAnalysisService codeAnalysisService;
    private final ParsingService parsingService;
    private final GitHubAppService gitHubAppService;
    private final GitHubCommentService gitHubCommentService;
    private final GitHubAppTokenService gitHubAppTokenService;

    @PostMapping(value = "/analyze-file-by-line", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<?>> postInlineComment(@RequestParam("file") MultipartFile file,
            @RequestParam() String owner,
            @RequestParam() String repo,
            @RequestParam() int pullNumber,
            @RequestParam() String prUrl,
            @RequestParam() String prAuthor,
            @RequestParam() String modelName) throws IOException {
        try {
            String diff = new String(file.getBytes(), StandardCharsets.UTF_8);
            String sha = gitHubAppService.getSha(owner, repo, pullNumber);

            AnalysisRequest request = new AnalysisRequest(
                    file.getOriginalFilename(),
                    diff,
                    String.valueOf(pullNumber),
                    sha,
                    prUrl,
                    prAuthor);

            return this.codeAnalysisService.analyzeDiff(request, modelName)
                    .handle((inlineReviewResponse, throwable) -> {
                        if (throwable != null) {
                            return this.showResponse((String) null, throwable, "Failure to analyze code by line.");
                        }

                        if (inlineReviewResponse == null) {
                            return ResponseEntity.ok().body("No new feedback generated or duplicate feedback skipped.");
                        }

                        try {
                            final String installationId = gitHubAppTokenService.getInstallationId(owner, repo);
                            final String token = gitHubAppTokenService
                                    .getInstallationToken(Long.parseLong(installationId));
                            log.info("The InlineReviewResponse: {}", inlineReviewResponse);

                            final List<GitDiff> diffsFromPr = this.gitHubAppService.getDiffs(owner, repo, pullNumber);
                            
                            List<Issue> issuesToPost = new java.util.ArrayList<>();

                            for (com.erik.git_bro.dto.Issue aiIssue : inlineReviewResponse.getIssues()) {
                                String issueFile = aiIssue.getFile();
                                int line = aiIssue.getLine();
                                String comment = aiIssue.getComment();

                                // Find the matching GitDiff
                                Optional<GitDiff> matchingDiff = diffsFromPr.stream()
                                        .filter(d -> {
                                            log.info("Comparing issueFile: {} with GitDiff filename: {}", issueFile,
                                                    d.getFilename());
                                            return d.getFilename().equals(issueFile);
                                        })
                                        .findFirst();

                                if (matchingDiff.isPresent()) {
                                    GitDiff gitDiff = matchingDiff.get();
                                    if (gitDiff.getPatch() == null || gitDiff.getPatch().isBlank()) {
                                        log.warn("Skipping comment: Diff patch is blank for file: {}", issueFile);
                                    } else {
                                        Set<Integer> validLines = this.parsingService
                                                .extractCommentableLines(gitDiff.getPatch());

                                        if (validLines.contains(line)) {
                                            Integer position = this.parsingService
                                                    .calculatePositionInDiffHunk(gitDiff.getPatch(), line);
                                            if (position != null) {
                                                gitHubCommentService.postBlockComments(
                                                        token,
                                                        owner,
                                                        repo,
                                                        pullNumber,
                                                        issueFile,
                                                        position, // Use position here
                                                        comment,
                                                        sha);
                                                issuesToPost.add(new Issue(issueFile, line, position, comment));
                                            } else {
                                                log.warn(
                                                        "Skipping comment: Could not calculate position for line {} in {}.",
                                                        line, issueFile);
                                            }
                                        } else {
                                            log.warn("Skipping comment: line {} in {} is not part of diff.", line,
                                                    issueFile);
                                        }
                                    }
                                } else {
                                    log.warn("No diff found for file: {}", issueFile);
                                }
                            }

                            StringBuilder markdownSummary = new StringBuilder();
                            markdownSummary.append("### 🤖 AI Review Summary\n");
                            markdownSummary.append("Posted ").append(issuesToPost.size())
                                    .append(" inline comments.\n\n");

                            for (Issue issue : issuesToPost) {
                                markdownSummary
                                        .append("- **File**: `").append(issue.getFile()).append("`\n")
                                        .append("  - **Line**: ").append(issue.getLine()).append("\n")
                                        .append("  - **Comment**: ").append(issue.getComment().replaceAll("\n", " ").trim())
                                        .append("\n\n");
                            }
                            markdownSummary.append("**Recommendation**: ").append(inlineReviewResponse.getRecommendation()).append("\n");

                        //    this.gitHubCommentService.postReviewCommentBatch(token, owner, repo, pullNumber, issuesToPost);

                            return ResponseEntity.ok()
                                    .body(markdownSummary.toString().trim());

                        } catch (Exception e) {
                            log.error("Failed to parse or post inline comments", e);
                            return ResponseEntity.status(500).body("Failed to post inline comments: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to get SHA or prepare analysis request", e);
            return CompletableFuture
                    .completedFuture(ResponseEntity.status(500).body("Failed to prepare analysis: " + e.getMessage()));
        }
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
