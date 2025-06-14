package com.erik.git_bro.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Bean(name = "virtualThreadExecutor")
    public Executor getAsyncExectuor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
