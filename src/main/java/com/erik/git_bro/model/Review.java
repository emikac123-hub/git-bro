package com.erik.git_bro.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedEntityGraph;
import lombok.Builder;
import lombok.Data;

/**
 * Entity representing a code review for a pull request.
 * <p>
 * Stores information about the pull request, the file path analyzed,
 * the original diff content, the AI-generated feedback, and the creation timestamp.
 * </p>
 * <p>
 * This entity is persisted in the database with large object (CLOB) support
 * for diff content and feedback fields.
 * </p>
 * <p>
 * The {@code Review} entity uses {@code @NamedEntityGraph} to allow
 * specification of fetch graphs if needed (currently no attributes specified).
 * </p>
 * 
 * @author erikmikac
 */
@Data
@Entity
@Builder
@NamedEntityGraph(name = "Review.all", attributeNodes = {})
public class Review {

    /**
     * The unique identifier for the review record.
     * This is generated automatically by the persistence provider.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the pull request associated with this review.
     * This may be null if not set.
     */
    private String pullRequestId;

    /**
     * The file path of the source file related to the review.
     */
    private String filePath;

    /**
     * The original diff content of the code changes being reviewed.
     * Stored as a Character Large Object (CLOB) in the database.
     */
    @Lob
    @Column(name = "diff_content", columnDefinition = "CLOB")
    private String diffContent;

    /**
     * The AI-generated feedback or comments on the code changes.
     * Stored as a Character Large Object (CLOB) in the database.
     */
    @Lob
    @Column(name = "feedback", columnDefinition = "CLOB")
    private String feedback;

    /**
     * Timestamp of when the review was created.
     */
    private Instant createdAt;
}
