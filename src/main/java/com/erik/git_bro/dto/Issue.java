package com.erik.git_bro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Issue {
    private String file;
    private int line; // Absolute line number
    private int position; // Line number within the diff hunk
    private String comment;
}
