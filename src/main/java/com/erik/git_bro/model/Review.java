package com.erik.git_bro.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "review", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pullRequestId", "feedbackFingerprint"})
})
public class Review {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    
    @Column(nullable = false)
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String diffContent;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private Integer line;

    @Column(nullable = false)
    private String pullRequestId;

    @Column(nullable = false, unique = true)
    private String feedbackFingerprint;

    private String userId;

    private String prUrl;

    private Boolean issueFlag;

    @Column(precision = 3, scale = 2)
    private BigDecimal severityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_iteration_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ReviewIteration reviewIteration;
}