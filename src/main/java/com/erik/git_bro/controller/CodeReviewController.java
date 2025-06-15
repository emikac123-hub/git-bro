package com.erik.git_bro.controller;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
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
        return codeAnalysisService.analyzeDiff(pullRequestId, diffContent)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    Throwable root = Optional.ofNullable(throwable.getCause()).orElse(throwable);

                    log.error("Error during code analysis", root);

                    String message = root.getMessage();
                    String details = Optional.ofNullable(root.getCause())
                            .map(Throwable::getLocalizedMessage)
                            .orElse("No additional details");

                    ErrorResponse error = ErrorResponse.builder()
                            .message(message)
                            .details(details)
                            .build();

                    if (root instanceof IllegalArgumentException) {
                        return ResponseEntity.badRequest().body(error);
                    }

                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });

    }
}
