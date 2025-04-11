package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoBatchConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoBatchConsumer.class);

    public VideoBatchConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_BATCH_PROCESSING_QUEUE)
    public void processVideoBatch(String batchIdStr) {
        executeWithRetry(() -> {
            try {
                UUID batchId = UUID.fromString(batchIdStr);
                logger.info("Processamento em lote {} processado com sucesso às: {}", batchId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", batchIdStr, e.getMessage());
                throw new RuntimeException(e); 
            } catch (Exception e) {
                logger.error("Erro ao processar lote de vídeos com ID '{}'. Detalhes: {}", batchIdStr, e.getMessage());
                throw new RuntimeException(e); 
            }
        });
    }
}