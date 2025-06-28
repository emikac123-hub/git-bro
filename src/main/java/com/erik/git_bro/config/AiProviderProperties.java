package com.erik.git_bro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the AI provider used in the application.
 * <p>
 * This class binds to properties prefixed with {@code app} in the application configuration,
 * allowing easy configuration and retrieval of the AI provider name or identifier.
 * </p>
 * <p>
 * Example property in application.yml or application.properties:
 * <pre>
 * app.ai-provider=chatgpt
 * </pre>
 * </p>
 */
@ConfigurationProperties(prefix = "app")
@Component
public class AiProviderProperties {

    /**
     * The name or identifier of the AI provider to be used.
     */
    private String aiProvider;
    private String gemini;

    /**
     * Returns the AI provider configured for the application.
     *
     * @return the AI provider name or identifier.
     */
    public String getAiProvider() {
        return aiProvider;
    }

    /**
     * Sets the AI provider to be used by the application.
     *
     * @param aiProvider the AI provider name or identifier.
     */
    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }
}
