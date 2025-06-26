package com.erik.git_bro.service.github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.springframework.stereotype.Service;

import com.erik.git_bro.dto.GitDiff;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class GitHubAppService {

    private final GitHubAppTokenService gitHubAppTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * List repositories accessible to the GitHub App Installation.
     * 
     * @throws Exception
     */
    public List<String> listInstallationRepos() throws Exception {
        final String installationToken = this.gitHubAppTokenService.getInstallationToken();
        final HttpClient client = HttpClient.newHttpClient();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/installation/repositories"))
                .header("Authorization", "token " + installationToken)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to receive installation repos: " + response.body());
        }
        final JsonNode jsonNode = objectMapper.readTree(response.body());
        final JsonNode repoNodes = jsonNode.get("repositories");

        return repoNodes.findValuesAsText("full_name");
    }

    public String getSha(final String owner, final String repo, final int pullNumber) throws Exception {
        final String installationId = this.gitHubAppTokenService.getInstallationId(owner, repo);
        final String token = this.gitHubAppTokenService.getInstallationToken(Long.parseLong(installationId));
        log.info("installationId: {}", installationId);
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/pulls/" + pullNumber))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get SHA: " + response.body());
        }

        final JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("head").get("sha").asText();
    }

    public List<GitDiff> getDiffs(final String owner, final String repo, final int pullNumber) throws Exception {
        final String installationId = this.gitHubAppTokenService.getInstallationId(owner, repo);
        final String token = this.gitHubAppTokenService.getInstallationToken(Long.parseLong(installationId));
        log.info("installationId: {}", installationId);
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.github.com/repos/" + owner + "/" + repo + "/pulls/" + pullNumber + "/files"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get Diff: " + response.body());
        }
        log.info("REPONSE: {}", response.body());

        final List<GitDiff> diffs = objectMapper.readValue(
                response.body(),
                new TypeReference<List<GitDiff>>() {
                });
        return diffs;
    }
}
