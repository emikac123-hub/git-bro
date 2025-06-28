package com.erik.git_bro.service;

import com.erik.git_bro.model.ReviewIteration;
import com.erik.git_bro.repository.ReviewIterationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewIterationService {

    private final ReviewIterationRepository reviewIterationRepository;

    /**
     * Finds an existing ReviewIteration for the given commit SHA or creates a new one if not found.
     * This method is transactional to ensure atomic read-and-write operations.
     *
     * @param commitSha The commit SHA to search for.
     * @return A managed ReviewIteration entity.
     */
    @Transactional
    public ReviewIteration findOrCreateIteration(String pullRequestId, String commitSha) {
        return reviewIterationRepository
            .findByPullRequestIdAndCommitSha(pullRequestId, commitSha)
            .orElseGet(() -> {
                log.info("No iteration found for PR: {} and commit SHA: {}. Creating a new one.", pullRequestId, commitSha);
                ReviewIteration newIteration = ReviewIteration.builder()
                    .pullRequestId(pullRequestId)
                    .commitSha(commitSha)
                    .pushAt(Instant.now())
                    .build();
                return reviewIterationRepository.save(newIteration);
            });
    }
}