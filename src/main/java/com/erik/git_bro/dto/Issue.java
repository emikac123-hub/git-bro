package com.erik.git_bro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Issue {
    private String file;
    private int line; // Absolute line number
    private int position; // Line number within the diff hunk
    private String comment;
}
