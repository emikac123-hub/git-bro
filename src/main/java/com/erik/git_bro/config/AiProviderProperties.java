package com.erik.git_bro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app")
@Component
public class AiProviderProperties {
    private String aiProvider;

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }
}
