package com.erik.git_bro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.erik.git_bro.ai.ChatGptAnalyzer;
import com.erik.git_bro.ai.CodeAnalyzer;
import com.erik.git_bro.ai.CodeBertAnalyzer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AnalyzerConfig {

    @Value("${app.ai-provider}")
    private String aiProvider;

    private final CodeBertAnalyzer codeBertAnalyzer;
    private final ChatGptAnalyzer chatGptAnalyzer;
    private final AiProviderProperties aiProviderProperties;

    public AnalyzerConfig(CodeBertAnalyzer codeBertAnalyzer, ChatGptAnalyzer chatGptAnalyzer, AiProviderProperties aiProviderProperties) {
        this.codeBertAnalyzer = codeBertAnalyzer;
        this.chatGptAnalyzer = chatGptAnalyzer;
        this.aiProviderProperties = aiProviderProperties;
    }

    @Bean
    public CodeAnalyzer codeAnalyzer() {
        final var provider = aiProviderProperties.getAiProvider().toLowerCase();
        log.info("Using {}", provider);
        return switch (provider) {
            case "chatgpt" -> chatGptAnalyzer;
            case "codebert" -> codeBertAnalyzer;
            default -> throw new IllegalArgumentException("Unknown AI provider: " + aiProvider);
        };
    }
}