package com.erik.git_bro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.erik.git_bro.config.AiProviderProperties;

/**
 * The main Spring Boot application class for GitBro.
 * <p>
 * This class bootstraps the application, enabling asynchronous
 * processing and binding configuration properties defined in {@link AiProviderProperties}.
 * </p>
 */
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AiProviderProperties.class)
public class GitBroApplication {

    /**
     * The main method used to run the Spring Boot application.
     * 
     * @param args runtime arguments (unused)
     */
    public static void main(String[] args) {
        SpringApplication.run(GitBroApplication.class, args);
    }
}
