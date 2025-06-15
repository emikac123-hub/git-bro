package com.erik.git_bro.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erik.git_bro.model.ErrorResponse;
import com.erik.git_bro.service.CodeAnalysisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Slf4j
public class CodeReviewController {

    private final CodeAnalysisService codeAnalysisService;

    @PostMapping("/analyze")
    public CompletableFuture<ResponseEntity<Object>> analyzeCode(
            @RequestParam String pullRequestId,
            @RequestBody String diffContent) {
        log.info("Received review request for PR {}. Diff length: {}", pullRequestId,
                diffContent.length());
        return codeAnalysisService.analyzeDiff(pullRequestId,  diffContent)
                .thenApply(review -> ResponseEntity.ok(review))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    // Replace with SLF4J logging in production
                    log.info("Eror!");
                    final var error = ErrorResponse.builder()
                            .message(cause.getMessage())
                            .details(cause.getStackTrace().toString())
                            .build();
                    if (cause instanceof IllegalArgumentException) {

                        return ResponseEntity.badRequest().body(error);
                    }
                    log.info(cause.getMessage());
                    return ResponseEntity.status(500).body(null);
                });
    }
}
