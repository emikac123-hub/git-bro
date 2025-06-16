package com.erik.git_bro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.erik.git_bro.ai.ChatGptAnalyzer;
import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.ai.CodeBertAnalyzer;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class that provides the appropriate {@link CodeAnalyzer} bean implementation
 * based on the configured AI provider.
 * <p>
 * The AI provider is specified via the {@code app.ai-provider} property, and can be either
 * {@code "chatgpt"} or {@code "codebert"} (case-insensitive).
 * </p>
 * <p>
 * This configuration enables easy switching between different AI analysis implementations
 * without changing application code.
 * </p>
 * 
 * <p>Example usage in application.properties or application.yml:</p>
 * <pre>
 * app.ai-provider=chatgpt
 * </pre>
 */
@Configuration
@Slf4j
public class AnalyzerConfig {

    @Value("${app.ai-provider}")
    private String aiProvider;

    private final CodeBertAnalyzer codeBertAnalyzer;
    private final ChatGptAnalyzer chatGptAnalyzer;
    private final AiProviderProperties aiProviderProperties;

    /**
     * Constructs the AnalyzerConfig with injected analyzers and properties.
     *
     * @param codeBertAnalyzer the CodeBERT implementation of {@link CodeAnalyzer}
     * @param chatGptAnalyzer the ChatGPT implementation of {@link CodeAnalyzer}
     * @param aiProviderProperties the properties class to retrieve configured AI provider
     */
    public AnalyzerConfig(CodeBertAnalyzer codeBertAnalyzer, ChatGptAnalyzer chatGptAnalyzer, AiProviderProperties aiProviderProperties) {
        this.codeBertAnalyzer = codeBertAnalyzer;
        this.chatGptAnalyzer = chatGptAnalyzer;
        this.aiProviderProperties = aiProviderProperties;
    }

    /**
     * Returns the {@link CodeAnalyzer} bean implementation according to the configured AI provider.
     * <p>
     * Supports "chatgpt" and "codebert" (case-insensitive). Throws {@link IllegalArgumentException}
     * if the provider is unknown.
     * </p>
     *
     * @return the selected {@link CodeAnalyzer} implementation bean
     * @throws IllegalArgumentException if the AI provider is not recognized
     */
    @Bean
    public CodeAnalyzer codeAnalyzer() {
        final var provider = aiProviderProperties.getAiProvider().toLowerCase();
        log.info("Using AI provider: {}", provider);
        return switch (provider) {
            case "chatgpt" -> chatGptAnalyzer;
            case "codebert" -> codeBertAnalyzer;
            default -> throw new IllegalArgumentException("Unknown AI provider: " + aiProvider);
        };
    }
}
