package com.erik.git_bro.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
