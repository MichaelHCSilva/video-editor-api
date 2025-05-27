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
public class VideoDownloadConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public VideoDownloadConsumer() {}

    @RabbitListener(queues = RabbitMQConfig.VIDEO_DOWNLOAD_QUEUE)
    public void processVideoDownload(@Payload String downloadIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID downloadId = UUID.fromString(downloadIdStr);
                logger.info("Download de vídeo {} processado com sucesso. Status atualizado às: {}", downloadId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", downloadIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_DOWNLOAD_DLQ, downloadIdStr);
                throw new RuntimeException("Erro no processamento do download de vídeo: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro ao processar download de vídeo com ID '{}'. Detalhes: {}", downloadIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_DOWNLOAD_DLQ, downloadIdStr);
                throw new RuntimeException("Erro no processamento do download de vídeo: " + e.getMessage(), e);
            }
        });
    }
}
