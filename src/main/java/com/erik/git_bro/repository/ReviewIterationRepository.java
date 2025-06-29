package com.erik.git_bro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erik.git_bro.model.ReviewIteration;

public interface ReviewIterationRepository extends JpaRepository<ReviewIteration, UUID>{
    @EntityGraph(value = "ReviewIteration.all")
    List<ReviewIteration> findByPullRequestId(String pullRequestId);

    Optional<ReviewIteration> findByPullRequestIdAndCommitSha(String pullRequestId, String commitSha);

}
