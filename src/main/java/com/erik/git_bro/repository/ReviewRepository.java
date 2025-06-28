package com.erik.git_bro.repository;

import com.erik.git_bro.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Checks if a Review entity with the given fingerprint already exists for a specific pull request.
     *
     * @param pullRequestId the ID of the pull request
     * @param feedbackFingerprint the unique fingerprint of the feedback
     * @return true if a matching review exists, false otherwise
     */
    boolean existsByPullRequestIdAndFeedbackFingerprint(String pullRequestId, String feedbackFingerprint);
}