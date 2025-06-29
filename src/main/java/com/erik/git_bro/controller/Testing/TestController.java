package com.erik.git_bro.controller.Testing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erik.git_bro.model.Review;
import com.erik.git_bro.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/test")
@Slf4j
@RequiredArgsConstructor
public class TestController {

    private final ReviewRepository reviewRepository;
    @PostMapping("/review-data")
    public void postReviewData() {
                            BigDecimal score = new BigDecimal(0.1);
                            final var review = Review.builder()
                            .createdAt(Instant.now())
                            .fileName("file")
                            .prUrl(null)
                            .pullRequestId(null)
                            .issueFlag(null)
                            .diffContent("+ public static void main")
                            .userId("Erik")
                            // .aiModel(review.setAiModel(aiModelRepository.findById(aiModelId).orElseThrow(()
                            // -> log.err));)
                            .feedback((String) "Good Job")
                            .severityScore(score)
                            .build();
                    reviewRepository.save(review);
        
    }
    

}
