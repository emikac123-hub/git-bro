package com.erik.git_bro.controller;

import com.erik.git_bro.service.github.GitHubAppService;
import com.erik.git_bro.service.github.GitHubAppTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(GitHubAppController.class)
public class GitHubAppControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public GitHubAppService gitHubAppService() {
            return Mockito.mock(GitHubAppService.class);
        }

        @Bean
        public GitHubAppTokenService gitHubAppTokenService() {
            return Mockito.mock(GitHubAppTokenService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubAppService gitHubAppService;

    @Autowired
    private GitHubAppTokenService gitHubAppTokenService;

    @Test
    @WithMockUser
    public void testGetInstallationToken() throws Exception {
        when(gitHubAppTokenService.getInstallationToken()).thenReturn("test-token");
        mockMvc.perform(get("/api/github/token")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testListRepos() throws Exception {
        when(gitHubAppService.listInstallationRepos()).thenReturn(Collections.singletonList("test-repo"));
        mockMvc.perform(get("/api/github/repos")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetSha() throws Exception {
        when(gitHubAppService.getSha("emikac123-hub", "git-bro", 11)).thenReturn("test-sha");
        mockMvc.perform(get("/api/github/sha")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetDiffs() throws Exception {
        when(gitHubAppService.getDiffs("emikac123-hub", "git-bro", 11)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/github/diff-files")).andExpect(status().isOk());
    }
}
