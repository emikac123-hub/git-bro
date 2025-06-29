package com.erik.git_bro.service.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class GitHubCommentServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public OkHttpClient okHttpClient() {
            return Mockito.mock(OkHttpClient.class);
        }
    }

    @Autowired
    private GitHubCommentService gitHubCommentService;

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testPostBlockComments() throws IOException {
        // Mock OkHttpClient behavior
    }

    @Test
    public void testPostReviewCommentBatch() throws IOException {
        // Mock OkHttpClient behavior
    }
}
