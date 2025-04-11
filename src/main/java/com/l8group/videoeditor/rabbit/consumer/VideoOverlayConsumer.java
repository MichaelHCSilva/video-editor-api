package com.l8group.videoeditor.rabbit.consumer;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoOverlayConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayConsumer.class);

    public VideoOverlayConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_OVERLAY_QUEUE)
    public void processVideoOverlay(String videoOverlayIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoOverlayId = UUID.fromString(videoOverlayIdStr);
                logger.info("Overlay de vídeo {} processado com sucesso. Status atualizado às: {}", videoOverlayId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoOverlayIdStr, e.getMessage());
                throw new RuntimeException(e); 
            } catch (Exception e) {
                logger.error("Erro ao processar overlay de vídeo com ID '{}'. Detalhes: {}", videoOverlayIdStr, e.getMessage());
                throw new RuntimeException(e); 
            }
        });
    }
}