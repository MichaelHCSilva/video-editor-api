package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoProcessingConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public VideoProcessingConsumer() {}

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSING_QUEUE)
    public void processVideo(@Payload String videoIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID videoId = UUID.fromString(videoIdStr);
                logger.info("Vídeo {} processado com sucesso. Processado às: {}", videoId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_PROCESSING_DLQ, videoIdStr);
                throw new RuntimeException("Erro na conversão do UUID do vídeo: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro ao processar vídeo com ID '{}'. Detalhes: {}", videoIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_PROCESSING_DLQ, videoIdStr);
                throw new RuntimeException("Erro inesperado no processamento do vídeo: " + e.getMessage(), e);
            }
        });
    }
}
