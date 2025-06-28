package com.erik.git_bro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Issue {
    private String file;
    private int line;
    private String comment;
}
