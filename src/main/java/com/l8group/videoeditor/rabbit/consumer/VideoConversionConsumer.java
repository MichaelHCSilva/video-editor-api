package com.l8group.videoeditor.rabbit.consumer;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoConversionConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionConsumer.class);

    public VideoConversionConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_CONVERSION_QUEUE)
    public void processVideoConversion(String videoConversionIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoConversionId = UUID.fromString(videoConversionIdStr);
                logger.info("Conversão de vídeo {} processada com sucesso. Status atualizado às: {}", videoConversionId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoConversionIdStr, e.getMessage());
                throw new RuntimeException(e); 
            } catch (Exception e) {
                logger.error("Erro ao processar conversão de vídeo com ID '{}'. Detalhes: {}", videoConversionIdStr, e.getMessage());
                throw new RuntimeException(e); 
            }
        });
    }
}