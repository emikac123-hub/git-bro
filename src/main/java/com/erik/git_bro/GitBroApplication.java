package com.erik.git_bro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.erik.git_bro.config.AiProviderProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AiProviderProperties.class)
public class GitBroApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitBroApplication.class, args);
	}

}
