package com.erik.git_bro.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitDiff {

    private String filename;
    private String patch;
    private String status;
    private String deletions;
    private String additions;
    private String changes;
    private String sha;
}
