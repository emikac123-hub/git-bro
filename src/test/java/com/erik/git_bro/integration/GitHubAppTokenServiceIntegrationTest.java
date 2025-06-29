import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.erik.git_bro.TestUtils.DummyKeys;
import com.erik.git_bro.service.github.GitHubAppTokenService;
import com.erik.git_bro.util.ApiUrlProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class GitHubAppTokenServiceIntegrationTest {

    private MockWebServer mockWebServer;
    private GitHubAppTokenService gitHubAppTokenService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Create a test ApiUrlProvider that returns URLs based on the mock server URL
        ApiUrlProvider testApiUrlProvider = new ApiUrlProvider() {
            @Override
            public String getInstallationTokenUrl(long installationId) {
                // This URL points to the mock server + path expected by your service
                return mockWebServer.url("/app/installations/" + installationId + "/access_tokens").toString();
            }

            @Override
            public String getInstallationIdUrl(String owner, String repo) {
                return mockWebServer.url("/repos/" + owner + "/" + repo + "/installation").toString();
            }

            // Add other methods if needed...
        };

        gitHubAppTokenService = new GitHubAppTokenService(testApiUrlProvider);

        // Set other private fields if needed
        ReflectionTestUtils.setField(gitHubAppTokenService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(gitHubAppTokenService, "publicPem", DummyKeys.VALID_PUBLIC_KEY);
        ReflectionTestUtils.setField(gitHubAppTokenService, "privatePem", DummyKeys.VALID_PRIVATE_PEM);
        ReflectionTestUtils.setField(gitHubAppTokenService, "appId", "12345");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetInstallationToken_Success() throws Exception {
        String fakeToken = "mocked_installation_token";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"token\":\"" + fakeToken + "\"}")
                .addHeader("Content-Type", "application/json"));

        String token = gitHubAppTokenService.getInstallationToken();

        assertEquals(fakeToken, token);
    }
}
