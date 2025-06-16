package com.erik.git_bro.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.erik.git_bro.model.ErrorResponse;
import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.ReviewRepository;
import com.erik.git_bro.service.CodeAnalysisService;
import com.erik.git_bro.service.ParsingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/review")
@Slf4j
public class CodeReviewController {

    private final ReviewRepository reviewRepository;
    private final ParsingService parsingService;
    CodeAnalysisService codeAnalysisService;

    CodeReviewController(CodeAnalysisService codeAnalysisService, ReviewRepository reviewRepository, final ParsingService parsingService) {
        this.codeAnalysisService = codeAnalysisService;
        this.reviewRepository = reviewRepository;
        this.parsingService = parsingService;
    }

    @PostMapping(value = "/analyze-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<?>> analyzeFromFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        String diff = new String(file.getBytes(), StandardCharsets.UTF_8);

        return codeAnalysisService.analyzeFile(file.getOriginalFilename(), diff)
                .handle((feedback, throwable) -> {
                    if (throwable == null) {
                        final var review = Review.builder()
                        .createdAt(Instant.now())
                        .filePath(this.parsingService.extractFilePathFromDiff(diff))
                        .diffContent(diff)
                        .feedback((String) feedback)
                        .build();
                        this.reviewRepository.save(review);
                        return ResponseEntity.ok().body(feedback);
                    }

                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    log.error("Code analysis failed", cause);

                    var error = ErrorResponse.builder()
                            .message(cause.getMessage())
                            .details(cause.getCause() != null ? cause.getCause().getLocalizedMessage() : "")
                            .build();

                    if (cause instanceof IllegalArgumentException) {
                        return ResponseEntity.badRequest().body(error);
                    }

                    return ResponseEntity.status(500).body(error);
                });
    }

}
