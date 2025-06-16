package com.erik.git_bro.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.erik.git_bro.model.ErrorResponse;
import com.erik.git_bro.service.CodeAnalysisService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller that handles code review requests.
 * <p>
 * Provides an endpoint to receive a file containing code diffs and analyze them asynchronously using
 * a code analysis service.
 * </p>
 */
@RestController
@RequestMapping("/api/review")
@Slf4j
public class CodeReviewController {

    private final CodeAnalysisService codeAnalysisService;

    /**
     * Constructs a new {@code CodeReviewController} with the given {@link CodeAnalysisService}.
     *
     * @param codeAnalysisService the service used to perform asynchronous code analysis
     */
    public CodeReviewController(CodeAnalysisService codeAnalysisService) {
        this.codeAnalysisService = codeAnalysisService;
    }

    /**
     * Analyzes the contents of a file asynchronously.
     * <p>
     * Accepts a multipart form upload with a file parameter named "file". The file contents are read
     * as a UTF-8 string representing a code diff. The diff is then passed to the {@code CodeAnalysisService}
     * for asynchronous analysis.
     * </p>
     * <p>
     * The method returns a {@link CompletableFuture} that resolves to an HTTP response:
     * <ul>
     *   <li>{@code 200 OK} with the analysis feedback if successful</li>
     *   <li>{@code 400 Bad Request} with an {@link ErrorResponse} if the analysis failed due to invalid input</li>
     *   <li>{@code 500 Internal Server Error} with an {@link ErrorResponse} for other failures</li>
     * </ul>
     * </p>
     *
     * @param file the multipart uploaded file containing the code diff to analyze
     * @return a {@link CompletableFuture} that resolves to a {@link ResponseEntity} containing
     *         the analysis result or error information
     * @throws IOException if reading the uploaded file bytes fails
     */
    @PostMapping(value = "/analyze-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<?>> analyzeFromFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        String diff = new String(file.getBytes(), StandardCharsets.UTF_8);

        return codeAnalysisService.analyzeFile(file.getOriginalFilename(), diff)
                .handle((feedback, throwable) -> {
                    if (throwable == null) {
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
