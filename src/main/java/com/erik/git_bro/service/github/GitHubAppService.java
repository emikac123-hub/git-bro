package com.erik.git_bro.service.github;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@AllArgsConstructor
public class GitHubAppService {

    private final GitHubAppTokenService gitHubAppTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // got that here: https://github.com/settings/installations/71819645
    private final String INSTALLATION_ID = "71819645";

    /**
     * Exchange the app JWT for an installation access token. This is for my own app.
     * 
     * @throws Exception
     */
    public String getInstallationToken() throws Exception {
        final String jwt = gitHubAppTokenService.createJwtToken();

        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/app/installations/" + INSTALLATION_ID + "/access_tokens"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to get the installation token: " + response.body());

        }
        final JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("token").asText();
    }

    /**
     * Exchange the app JWT for an installation access token
     * 
     * @throws Exception
     */
    public String getInstallationId(final String owner, final String repo) throws Exception {
        final String jwt = gitHubAppTokenService.createJwtToken();

        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/installation"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to get the installation token: " + response.body());

        }
        final JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("token").asText();
    }

    public String getInstallationToken(final long installationId) throws Exception {
        final String jwt = gitHubAppTokenService.createJwtToken();

        final HttpClient client = HttpClient.newHttpClient();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/app/installation/" + installationId + "/access_tokens"))
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to get installation token: " + response.body());
        }

        final JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("token").asText();
    }

    /**
     * List repositories accessible to the GitHub App Installation.
     * 
     * @throws Exception
     */
    public List<String> listInstallationRepos() throws Exception {
        final String installationToken = getInstallationToken();
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
}
