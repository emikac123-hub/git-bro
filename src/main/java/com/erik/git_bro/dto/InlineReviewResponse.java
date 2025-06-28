package com.erik.git_bro.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InlineReviewResponse {
    private List<Issue> issues;
    private String recommendation;
}
