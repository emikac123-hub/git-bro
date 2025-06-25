package com.erik.git_bro.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erik.git_bro.dto.GitDiff;
import com.erik.git_bro.service.github.GitHubAppService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubAppController {
    private final GitHubAppService gitHubAppService;

    @GetMapping("/token")
    public ResponseEntity<String> getInstallationToken() throws Exception {
        try {
            String token = gitHubAppService.getInstallationToken();
            return ResponseEntity.ok(token);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body("Failed to get installation token: " + e.getMessage());
        }
    }

    @GetMapping("/repos")
    public ResponseEntity<?> listRepos() throws Exception {
        try {
            List<String> repos = gitHubAppService.listInstallationRepos();
            return ResponseEntity.ok(repos);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body("Failed to list repos: " + e.getMessage());
        }
    }


    @GetMapping("/sha")
    public ResponseEntity<?> getSha() throws Exception {
        try {
            final String sha = gitHubAppService.getSha("emikac123-hub", "git-bro", 11);
            return ResponseEntity.ok(sha);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body("Failed to get Sha: " + e.getMessage());
        }
    }

    /**
     * Method to retrieve the Diff deltas from a given pull request. This is needed for inline commenting. It's not enough to just
     * attach the gitdiff file. Sometimes, AI will try to write comments on line that are not included in the diff, which leads to a 422 error.
     * {"message":"Validation Failed","errors":[{"resource":"PullRequestReviewComment","code":"custom","field":"pull_request_review_thread.line","message":"pull_request_review_thread.line must be part of the diff"},{"resource":"PullRequestReviewComment","code":"missing_field","field":"pull_request_review_thread.diff_hunk"}],"documentation_url":"https://docs.github.com/rest/pulls/comments#create-a-review-comment-for-a-pull-request","status":"422"}
     * @return A list of Git Diffs
     * @throws Exception 
     */
    @GetMapping("diff-files")
    public ResponseEntity<?> getDiffs() throws Exception {
       try {
            final List<GitDiff> diffs = gitHubAppService.getDiffs("emikac123-hub", "git-bro", 11);
            return ResponseEntity.ok(diffs);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body("Failed to list repos: " + e.getMessage());
        }
    }
}
