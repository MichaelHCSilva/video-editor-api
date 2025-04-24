package com.l8group.videoeditor.rabbit.consumer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class VideoResizeConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeConsumer.class);

    public VideoResizeConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_RESIZE_QUEUE)
    public void processVideoResize(String videoResizeIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoResizeId = UUID.fromString(videoResizeIdStr);
                logger.info("Redimensionamento de vídeo {} processado com sucesso. Status atualizado às: {}", videoResizeId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoResizeIdStr, e.getMessage());
                throw new RuntimeException(e); 
            } catch (Exception e) {
                logger.error("Erro ao processar redimensionamento de vídeo com ID '{}'. Detalhes: {}", videoResizeIdStr, e.getMessage());
                throw new RuntimeException(e); 
            }
        });
    }
}