/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.erik.git_bro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedEntityGraph;
import lombok.Data;

/**
 *
 * @author erikmikac
 */
@Data
@Entity
@NamedEntityGraph(name = "Review.all", attributeNodes={})
public class Review {

    @Id
    private String reviewId; // PR-Number + TimeStamp
    private String pullRequestId;
    private String filePath;
    @Lob
    @Column(name ="diff_content", columnDefinition = "CLOB")
    private String diffContent;
    @Lob
    @Column(name ="feedback", columnDefinition = "CLOB")
    private String feedback; // AI Generated Comment
    private String createdAt;
}
