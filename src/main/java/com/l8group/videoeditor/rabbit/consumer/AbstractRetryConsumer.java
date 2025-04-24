package com.l8group.videoeditor.rabbit.consumer;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.l8group.videoeditor.config.ConsumerRetryConfig;

public abstract class AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRetryConsumer.class);

    @Autowired
    private ConsumerRetryConfig retryConfig;

    protected void executeWithRetry(Runnable task) {
        int retryCount = 0;
        while (retryCount < retryConfig.getMaxRetries()) {
            try {
                task.run(); 
                return;
            } catch (Exception e) {
                retryCount++;
                if (retryCount < retryConfig.getMaxRetries()) {
                    logger.warn("Erro na tentativa {}. Tentando novamente em {} ms.", retryCount, retryConfig.getRetryDelayMs(), e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryConfig.getRetryDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread interrompida durante espera de retry. Detalhes:", ie);
                        return;
                    }
                } else {
                    logger.error("Erro após {} tentativas. Abandonando retry. Detalhes:", retryCount, e);
                }
            }
        }
    }
}
