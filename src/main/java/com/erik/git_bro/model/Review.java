package com.erik.git_bro.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedEntityGraph;
import lombok.Builder;
import lombok.Data;

/**
 * Entity representing a code review for a pull request.
 * <p>
 * Stores information about the pull request, the file path analyzed,
 * the original diff content, the AI-generated feedback, the creation timestamp,
 * and metadata such as model used and severity score.
 * </p>
 * <p>
 * This entity is persisted in the database with large object (CLOB) support
 * for diff content and feedback fields.
 * </p>
 * 
 * @author erikmikac
 */
@Data
@Entity
@Builder
@NamedEntityGraph(name = "Review.all", attributeNodes = {})
public class Review {

    /** Unique identifier for the review record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional ID of the pull request associated with this review. */
    private String pullRequestId;

    /** GitHub username or internal user identifier. */
    private String userId;

    /** URL to the pull request. */
    private String prUrl;

    /** File path of the source file related to the review. */
    private String fileName;

    /** Original diff content of the code changes. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String diffContent;

    /** AI-generated feedback. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /** AI model used for analysis (e.g., chatgpt, claude, codebert). */
    @ManyToOne
    @JoinColumn(name = "ai_model_id", nullable = true)
    private AiModel aiModel;

    /** Whether the AI flagged an issue (true = issue found). */
    private Boolean issueFlag;

    /** Optional severity score (0.0–1.0) for issue impact. */
    private BigDecimal severityScore;

    /** Timestamp when the review was created. */
    private Instant createdAt;
}
