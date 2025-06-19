package com.erik.git_bro.dto;

import lombok.Data;

@Data
public class Issue {
    private String file;
    private int line;
    private String comment;
}
