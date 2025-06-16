/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

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
 *
 * @author erikmikac
 */
@Data
@Entity
@Builder
@NamedEntityGraph(name = "Review.all", attributeNodes={})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // or AUTO, SEQUENCE depending on DB
    private Long id;
    private String pullRequestId;
    private String filePath;
    @Lob
    @Column(name ="diff_content", columnDefinition = "CLOB")
    private String diffContent;
    @Lob
    @Column(name ="feedback", columnDefinition = "CLOB")
    private String feedback; // AI Generated Comment
    private Instant createdAt;
}
