package com.l8group.videoeditor.rabbit.consumer;

import java.time.ZonedDateTime;
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
public class VideoOverlayConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public VideoOverlayConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_OVERLAY_QUEUE)
    public void processVideoOverlay(@Payload String videoOverlayIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID videoOverlayId = UUID.fromString(videoOverlayIdStr);
                logger.info("Overlay de vídeo {} processado com sucesso. Status atualizado às: {}", videoOverlayId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoOverlayIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_OVERLAY_DLQ, videoOverlayIdStr);
                throw new RuntimeException("Erro no processamento do overlay de vídeo: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro ao processar overlay de vídeo com ID '{}'. Detalhes: {}", videoOverlayIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_OVERLAY_DLQ, videoOverlayIdStr);
                throw new RuntimeException("Erro no processamento do overlay de vídeo: " + e.getMessage(), e);
            }
        });
    }
}
