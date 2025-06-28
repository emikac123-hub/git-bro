package com.erik.git_bro.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIterationDTO {
    private Long id;
    private Long reviewId;
    private String commitSha;
    private Instant pushAt;
    private String aiModel;
    private BigDecimal aiTemperature;
    private String commentSummary;
    private String severityScore;
}