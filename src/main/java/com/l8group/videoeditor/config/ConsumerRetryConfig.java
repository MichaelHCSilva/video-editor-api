package com.l8group.videoeditor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerRetryConfig {

    @Value("${video.consumer.retry.max-retries}")
    private int maxRetries;

    @Value("${video.consumer.retry.delay-ms}")
    private long retryDelayMs;

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }
}