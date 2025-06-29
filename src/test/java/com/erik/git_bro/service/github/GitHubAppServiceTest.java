package com.erik.git_bro.service.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class GitHubAppServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public GitHubAppTokenService gitHubAppTokenService() {
            return Mockito.mock(GitHubAppTokenService.class);
        }
    }

    @Autowired
    private GitHubAppService gitHubAppService;

    @Autowired
    private GitHubAppTokenService gitHubAppTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testListInstallationRepos() throws Exception {
        when(gitHubAppTokenService.getInstallationToken()).thenReturn("test-token");
        // Mock http client
    }

    @Test
    public void testGetSha() throws Exception {
        when(gitHubAppTokenService.getInstallationId(any(), any())).thenReturn("123");
        when(gitHubAppTokenService.getInstallationToken(any(Long.class))).thenReturn("test-token");
        // Mock http client
    }

    @Test
    public void testGetDiffs() throws Exception {
        when(gitHubAppTokenService.getInstallationId(any(), any())).thenReturn("123");
        when(gitHubAppTokenService.getInstallationToken(any(Long.class))).thenReturn("test-token");
        // Mock http client
    }
}
