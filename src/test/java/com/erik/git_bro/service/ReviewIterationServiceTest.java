package com.erik.git_bro.service;

import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewIterationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ReviewIterationServiceTest {

    @MockBean
    private ReviewIterationRepository reviewIterationRepository;

    @Autowired
    private ReviewIterationService reviewIterationService;

    @Test
    @WithMockUser
    public void testFindOrCreateIteration() {
        when(reviewIterationRepository.findByPullRequestIdAndCommitSha(any(), any())).thenReturn(Optional.of(new ReviewIteration()));
        reviewIterationService.findOrCreateIteration("1", "test-sha");

        when(reviewIterationRepository.findByPullRequestIdAndCommitSha(any(), any())).thenReturn(Optional.empty());
        when(reviewIterationRepository.save(any())).thenReturn(new ReviewIteration());
        reviewIterationService.findOrCreateIteration("1", "test-sha");
    }
}
