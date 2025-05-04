package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Service
public class VideoBatchConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoBatchConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public VideoBatchConsumer() {}

    @RabbitListener(queues = RabbitMQConfig.VIDEO_BATCH_PROCESSING_QUEUE)
    public void processVideoBatch(@Payload String batchIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID batchId = UUID.fromString(batchIdStr);
                logger.info("Processamento em lote {} processado com sucesso às: {}", batchId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", batchIdStr, e.getMessage());
                // Enviar explicitamente para a DLQ após falha crítica
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_BATCH_PROCESSING_DLQ, batchIdStr);
                throw new RuntimeException("Erro no processamento do lote de vídeos: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro ao processar lote de vídeos com ID '{}'. Detalhes: {}", batchIdStr, e.getMessage());
                // Enviar explicitamente para a DLQ após falha crítica
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_BATCH_PROCESSING_DLQ, batchIdStr);
                throw new RuntimeException("Erro no processamento do lote de vídeos: " + e.getMessage(), e);
            }
        });
    }
}
