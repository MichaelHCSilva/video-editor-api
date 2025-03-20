package com.l8group.videoeditor.rabbit.consumer;

import com.l8group.videoeditor.config.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

public abstract class AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRetryConsumer.class);

    @Autowired
    private RetryConfig retryConfig;

    protected void executeWithRetry(Runnable task) {
        int retryCount = 0;
        while (retryCount < retryConfig.getMaxRetries()) {
            try {
                if (retryCount > 0) {
                    TimeUnit.MILLISECONDS.sleep(retryConfig.getRetryDelayMs());
                }
                task.run();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted during sleep: {}", e.getMessage());
                return;
            } catch (Exception e) {
                retryCount++;
                logger.error("Erro na tentativa {}. Detalhes: {}", retryCount, e.getMessage());
                if (retryCount >= retryConfig.getMaxRetries()) {
                    logger.error("Erro após várias tentativas.");
                    return;
                }
            }
        }
    }
}