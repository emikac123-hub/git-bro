package com.erik.git_bro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erik.git_bro.model.Review;

public interface ReviewRepository extends JpaRepository<Review, String>{
    @EntityGraph(value = "Review.all")
    List<Review> findByPullRequestId(String pullRequestId);

}
