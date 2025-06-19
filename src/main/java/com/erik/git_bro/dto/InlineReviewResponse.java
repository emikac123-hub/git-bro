package com.erik.git_bro.dto;

import java.util.List;

import lombok.Data;

@Data
public class InlineReviewResponse {
    private List<Issue> issues;
    private String recommendation;
}
