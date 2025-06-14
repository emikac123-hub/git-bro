package com.erik.git_bro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GitBroApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitBroApplication.class, args);
	}

}
