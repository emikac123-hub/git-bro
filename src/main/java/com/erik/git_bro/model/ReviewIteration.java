package com.erik.git_bro.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIteration {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String pullRequestId;

    // The commit SHA this analysis was run against
    @Column(nullable = false)
    private String commitSha;

    // The timestamp of the push/analysis
    @Column(nullable = false)
    private Instant pushAt;

    @Column(precision = 3, scale = 2)
    private BigDecimal derivedSeverityScore;

    @Column
    private String aiModel;

    @Column(columnDefinition = "TEXT")
    private String commentSummary;

    // Each iteration can have multiple review comments.
    // 'mappedBy' indicates that the Review entity owns the relationship.
    @OneToMany(mappedBy = "reviewIteration", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews = new ArrayList<>();

    // Helper method to add a review to the iteration
    public void addReview(Review review) {
        reviews.add(review);
        review.setReviewIteration(this);
    }
}