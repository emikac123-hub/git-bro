package com.erik.git_bro.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erik.git_bro.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Checks if a Review entity with the given fingerprint already exists for a specific pull request.
     *
     * @param pullRequestId the ID of the pull request
     * @param feedbackFingerprint the unique fingerprint of the feedback
     * @return true if a matching review exists, false otherwise
     */
    boolean existsByPullRequestIdAndFeedbackFingerprint(String pullRequestId, String feedbackFingerprint);

    boolean existsByPullRequestId(String pullRequestId);
}