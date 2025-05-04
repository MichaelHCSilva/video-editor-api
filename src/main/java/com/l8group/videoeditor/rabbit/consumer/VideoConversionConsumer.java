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
public class VideoConversionConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public VideoConversionConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_CONVERSION_QUEUE)
    public void processVideoConversion(@Payload String videoConversionIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID videoConversionId = UUID.fromString(videoConversionIdStr);
                logger.info("Conversão de vídeo {} processada com sucesso. Status atualizado às: {}", videoConversionId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoConversionIdStr, e.getMessage());
                // Enviar explicitamente para a DLQ após falha crítica
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_CONVERSION_DLQ, videoConversionIdStr);
                throw new RuntimeException("Erro no processamento da conversão de vídeo: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro ao processar conversão de vídeo com ID '{}'. Detalhes: {}", videoConversionIdStr, e.getMessage());
                // Enviar explicitamente para a DLQ após falha crítica
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_CONVERSION_DLQ, videoConversionIdStr);
                throw new RuntimeException("Erro no processamento da conversão de vídeo: " + e.getMessage(), e);
            }
        });
    }
}
